package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.base.configs.SystemConst;
import cn.beinet.core.utils.FileHelper;
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
    private static final String CONFIG_FILE = "config.ini";

    /**
     * 是否启用文件管理
     */
    private boolean enabled;

    private List<String> dir;

    private boolean inited = false;

    // 记录配置文件最后修改时间
    private final AtomicLong lastModifiedTime = new AtomicLong(0);

    public FileManagerConfig() {
        init();
    }

    /**
     * 返回允许操作的根目录列表
     * @return 允许操作的根目录列表
     */
    public List<String> getDir() {
        if (dir == null || dir.isEmpty()) {
            throw new RuntimeException("yml里没有配置可访问的目录");
        }
        if (inited) {
            return dir;
        }

        if (!enabled) {
            dir = new ArrayList<>();
            return dir;
        }
        for (int i = 0, j = dir.size(); i < j; i++) {
            var item = dir.get(i);
            item = FileHelper.clearDirName(item);
            dir.set(i, item);
        }
        inited = true;
        return dir;
    }

    /**
     * 指定的目录，是否是允许操作的目录的子目录
     * @param usedDir 要操作的子目录
     * @return 是否允许操作
     */
    public boolean containsDir(String usedDir) {
        if (!enabled) {
            return false;
        }
        for (String dir : getDir()) {
            if (usedDir.startsWith(dir)) {
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
        String path = SystemConst.getBaseDir() + CONFIG_FILE;

        // 检查配置文件是否存在
        java.io.File configFile = new java.io.File(path);
        if (!configFile.exists()) {
            log.warn("配置文件不存在: {}", path);
            this.enabled = false;
            this.dir = new ArrayList<>();
            return;
        }

        // 更新最后修改时间
        long currentModifiedTime = configFile.lastModified();

        // 如果文件修改时间没有变化，不重新加载
        if (lastModifiedTime.get() == currentModifiedTime) {
            return;
        }

        Map<String, String> configs = readIni(path);

        // 读取是否启用文件管理
        String enabledStr = configs.get("enabled");
        this.enabled = enabledStr != null && enabledStr.equalsIgnoreCase("true");

        // 读取允许操作的目录列表
        String dirStr = configs.get("dirs");
        if (dirStr != null && !dirStr.isEmpty()) {
            dirStr = dirStr.replace('\\', '/')
                    .replace("//", "/");
            // 按逗号分隔目录
            this.dir = java.util.Arrays.stream(dirStr.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        } else {
            this.dir = new ArrayList<>();
        }

        // 更新最后修改时间
        lastModifiedTime.set(currentModifiedTime);
        this.inited = true;
        log.info("FileManagerConfig 初始化完成，enabled: {}, dirs: {}", enabled, dir);
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
}
