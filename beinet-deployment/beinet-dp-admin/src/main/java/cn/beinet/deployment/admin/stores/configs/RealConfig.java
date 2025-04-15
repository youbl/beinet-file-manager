package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.base.configs.SystemConst;
import cn.beinet.core.utils.FileHelper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实时配置
 */
@Component
@Slf4j
public final class RealConfig {
    // 配置文件所在路径
    @Value("${beinet.config.path:}")
    private String configFilePath;

    // 记录配置文件最后修改时间，用于避免频繁重复加载
    private final AtomicLong lastModifiedTime = new AtomicLong(0);
    private static Map<String, String> configMap = new HashMap<>();

    /**
     * 从实时配置里读取指定key的值
     * @param key key
     * @return val
     */
    public static String get(String key) {
        return get(key, "");
    }

    /**
     * 从实时配置里读取指定key的值
     * @param key key
     * @param defaultValue val为null时返回的默认值
     * @return val
     */
    public static String get(String key, String defaultValue) {
        String ret = configMap.get(key);
        return ret == null ? defaultValue : ret;
    }

    /**
     * 获取实时配置Integer值
     * @param key key
     * @return val
     */
    public static Integer getInt(String key) {
        return getInt(key, null);
    }

    /**
     * 获取实时配置Integer值，不存在时返回指定的默认值
     * @param key key
     * @return val
     */
    public static Integer getInt(String key, Integer defaultValue) {
        var ret = get(key);
        if (ret.isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(ret);
    }

    @PostConstruct
    void beanInit() {
        init();
    }

    // 每60秒（1分钟）执行一次配置刷新
    @Scheduled(fixedRate = 60000)
    public void refreshConfigs() {
        try {
            init();
        } catch (Exception e) {
            log.error("定时检查配置文件发生错误:", e);
        }
    }

    private void init() {
        String path = configFilePath;
        if (!StringUtils.hasText(path)) {
            path = SystemConst.getBaseDir() + "config.ini";
        }

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
        configMap = readIni(path);

        // 更新最后修改时间
        lastModifiedTime.set(currentModifiedTime);
        log.info("配置文件重新初始化完成: {}", configFile);
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
}
