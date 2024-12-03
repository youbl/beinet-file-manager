package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.utils.FileHelper;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 文件管理的配置类
 * @author youbl
 * @since 2024/12/3 17:14
 */
@Configuration
@ConfigurationProperties(prefix = "file-manager")
@Data
public class FileManagerConfig {
    /**
     * 是否启用文件管理
     */
    private boolean enabled;

    private List<String> dir;

    private boolean inited = false;

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
}
