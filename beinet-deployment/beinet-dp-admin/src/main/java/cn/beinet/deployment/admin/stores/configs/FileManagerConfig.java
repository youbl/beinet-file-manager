package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.base.configs.SystemConst;
import cn.beinet.core.utils.FileHelper;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件管理的配置类.
 * 从jar所在目录下读取ini文件
 *
 * @author youbl
 * @since 2024/12/3 17:14
 */
@Component
@Slf4j
public class FileManagerConfig {

    /**
     * 是否启用文件管理
     */
    private boolean enabled;

    /**
     * 允许访问的目录列表
     */
    private List<StoreInfo> dir;

    /**
     * 返回允许操作的根目录列表
     *
     * @return 允许操作的根目录列表
     */
    public List<StoreInfo> getDir() {
        if (dir == null || dir.isEmpty()) {
            throw new RuntimeException("没有配置可访问的目录");
        }
        return dir;
    }

    /**
     * 指定的目录，是否是允许读操作的目录的子目录
     *
     * @param usedDir 要操作的子目录
     * @return 是否允许读取
     */
    public boolean canReadDir(String usedDir) {
        if (!enabled || usedDir == null || usedDir.isEmpty()) {
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
     *
     * @param usedDir 要操作的子目录
     * @return 是否允许写入
     */
    public boolean canWriteDir(String usedDir) {
        if (!enabled || usedDir == null || usedDir.isEmpty()) {
            return false;
        }
        for (StoreInfo dir : getDir()) {
            if (!dir.isReadonly() && usedDir.startsWith(dir.getPath())) {
                return true;
            }
        }
        return false;
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
