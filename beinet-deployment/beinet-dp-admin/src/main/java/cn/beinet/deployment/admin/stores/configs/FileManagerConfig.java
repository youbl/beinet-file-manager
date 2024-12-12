package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.base.configs.SystemConst;
import cn.beinet.core.utils.FileHelper;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件管理的配置类.
 * 从jar所在目录下读取ini文件
 * @author youbl
 * @since 2024/12/3 17:14
 */
@Component
@Slf4j
public class FileManagerConfig {
    private static final String CONFIG_FILE = SystemConst.getBaseDir() + "config.ini";

    /**
     * 是否启用文件管理
     */
    private boolean enabled;

    /**
     * 允许访问的目录列表
     */
    private List<StoreInfo> dir;

    // 记录配置文件最后修改时间
    private final AtomicLong lastModifiedTime = new AtomicLong(0);

    public FileManagerConfig() {
        init();
    }

    /**
     * 返回允许操作的根目录列表
     * @return 允许操作的根目录列表
     */
    public List<StoreInfo> getDir() {
        if (dir == null || dir.isEmpty()) {
            throw new RuntimeException(CONFIG_FILE + "没有配置可访问的目录");
        }
        return dir;
    }

    /**
     * 指定的目录，是否是允许读操作的目录的子目录
     * @param usedDir 要操作的子目录
     * @return 是否允许读取
     */
    public boolean canReadDir(String usedDir) {
        if (!enabled) {
            return false;
        }
        for (StoreInfo dir : getDir()) {
            if (usedDir.startsWith(dir.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定的目录，是否是允许写入操作的目录的子目录
     * @param usedDir 要操作的子目录
     * @return 是否允许写入
     */
    public boolean canWriteDir(String usedDir) {
        if (!enabled) {
            return false;
        }
        for (StoreInfo dir : getDir()) {
            if (!dir.isReadonly() && usedDir.startsWith(dir.getPath())) {
                return true;
            }
        }
        return false;
    }

    @Scheduled(fixedRate = 60000) // 每60秒（1分钟）执行一次
    public void init() {
        try {
            checkConfigFileChanged();
        } catch (Exception e) {
            log.error("定时检查配置文件发生错误:", e);
        }
    }

    @SneakyThrows
    private void checkConfigFileChanged() {
        String path = CONFIG_FILE;

        // 检查配置文件是否存在
        java.io.File configFile = new java.io.File(path);
        if (!configFile.exists()) {
            log.warn("配置文件不存在: {}", path);
            this.enabled = false;
            this.dir = new ArrayList<>();
            return;
        }

        // 如果文件修改时间没有变化，不重新加载
        long currentModifiedTime = configFile.lastModified();
        if (currentModifiedTime <= lastModifiedTime.get()) {
            return;
        }

        // 从文件读取配置，并存入当前类的字段
        initFromIni(path);

        // 更新最后修改时间
        lastModifiedTime.set(currentModifiedTime);
        log.info("FileManagerConfig 初始化完成，enabled: {}, dirs: {}", enabled, dir);
    }

    private void initFromIni(String path) {
        Map<String, String> configs = readIni(path);

        // 读取是否启用文件管理
        String enabledStr = configs.get("enabled");
        this.enabled = enabledStr != null && enabledStr.equalsIgnoreCase("true");

        Map<String, StoreInfo> storeInfos = new HashMap<>();

        // 读取允许编辑操作的目录列表，防止同一个目录又可编辑，又设置只读
        String writableDirStr = configs.get("write-dir");
        List<StoreInfo> tmpWritable = toStoreInfoList(writableDirStr, false);
        putInMap(storeInfos, tmpWritable);

        // 读取允许只读操作的目录列表
        String readonlyDirStr = configs.get("read-dir");
        List<StoreInfo> tmpReadonly = toStoreInfoList(readonlyDirStr, true);
        putInMap(storeInfos, tmpReadonly);

        this.dir = storeInfos.values().stream().toList();
    }

    private static Map<String, String> readIni(String path) {
        // 使用 Properties 读取 INI 文件，会导致反斜杠丢失
//        java.util.Properties props = new java.util.Properties();
//        try (java.io.FileInputStream fis = new java.io.FileInputStream(new java.io.File(path))) {
//            props.load(fis);
//        }

        Map<String, String> map = new HashMap<>();

        String content = FileHelper.readFile(path);
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int idx = line.indexOf(':');
            if (idx <= 0 || idx >= line.length() - 1) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            map.put(key, value);
        }
        return map;
    }

    // 把逗号分隔的目录列表，转换为StoreInfo数组返回
    private List<StoreInfo> toStoreInfoList(String dirStr, boolean readonly) {
        if (dirStr == null || dirStr.isEmpty()) {
            return null;
        }
        dirStr = dirStr.replace('\\', '/')
                .replace("//", "/");

        // 按逗号分隔目录
        return java.util.Arrays.stream(dirStr.split(","))
                .map(item -> convertToStoreInfo(item, readonly))
                .filter(item -> !item.getName().isEmpty())
                .toList();
    }

    private void putInMap(Map<String, StoreInfo> map, List<StoreInfo> configs) {
        if (configs != null) {
            for (StoreInfo storeInfo : configs) {
                if (!map.containsKey(storeInfo.getName())) {
                    map.put(storeInfo.getName(), storeInfo);
                }
            }
        }
    }

    private StoreInfo convertToStoreInfo(String dir, boolean readonly) {
        dir = FileHelper.clearDirName(dir);
        return new StoreInfo()
                .setDir(true)
                .setReadonly(readonly)
                .setName(dir)
                .setPath(dir);
    }
}
