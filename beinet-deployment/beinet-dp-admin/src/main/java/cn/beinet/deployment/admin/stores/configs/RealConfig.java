package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.base.configs.SystemConst;
import cn.beinet.core.utils.FileHelper;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实时配置
 */
@Component
@Slf4j
public abstract class RealConfig {
    // 配置文件所在路径
    private static final String CONFIG_FILE = SystemConst.getBaseDir() + "config.ini";
    // 记录配置文件最后修改时间，用于避免频繁重复加载
    private final AtomicLong lastModifiedTime = new AtomicLong(0);
    private static Map<String, String> configMap = new HashMap<>();

    public RealConfig() {
        init();
    }

    // 每60秒（1分钟）执行一次配置刷新
    @Scheduled(fixedRate = 60000)
    public void refreshConfigs() {
        init();
    }


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
        log.info("配置文件重新初始化完成");
    }


    private void initFromIni(String path) {
        configMap = readIni(path);
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
            if (line.isEmpty() || line.startsWith("#")) {
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
                .filter(item -> !item.getName().isEmpty() && new File(item.getName()).isDirectory())
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

}
