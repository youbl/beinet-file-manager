package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.base.configs.SystemConst;
import cn.beinet.core.utils.FileHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    public FileManagerConfig() {
        try {
            init();
        } catch (Exception e) {
            log.error("FileManagerConfig init err:", e);
        }
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

    @SneakyThrows
    private void init() {
        String path = SystemConst.getBaseDir() + CONFIG_FILE;
        
        // 检查配置文件是否存在
        java.io.File configFile = new java.io.File(path);
        if (!configFile.exists()) {
            log.warn("配置文件不存在: {}", path);
            return;
        }

        // 使用 Properties 读取 INI 文件
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
            props.load(fis);
        }

        // 读取是否启用文件管理
        String enabledStr = props.getProperty("enabled", "false");
        this.enabled = Boolean.parseBoolean(enabledStr);

        // 读取允许操作的目录列表
        String dirStr = props.getProperty("dirs", "");
        if (dirStr != null && !dirStr.trim().isEmpty()) {
            // 按逗号分隔目录
            this.dir = java.util.Arrays.stream(dirStr.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        } else {
            this.dir = new ArrayList<>();
        }
    }
}
