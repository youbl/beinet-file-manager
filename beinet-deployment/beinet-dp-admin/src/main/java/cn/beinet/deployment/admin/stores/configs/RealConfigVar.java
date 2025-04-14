package cn.beinet.deployment.admin.stores.configs;

import cn.beinet.core.utils.FileHelper;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实时配置里的变量辅助类。
 *
 * @author youbl
 * @since 2025/4/14 16:34
 */
public abstract class RealConfigVar {

    public static List<String> activeDisks() {
        List<String> activeDisks = new ArrayList<>();
        String[] activeDiskConfig = RealConfig.get("active-disks").split(",");
        for (String activeDisk : activeDiskConfig) {
            activeDisk = activeDisk.trim();
            if (activeDisk.isEmpty()) {
                continue;
            }
            if (!activeDisk.contains(":")) {
                activeDisk += ":/";
            } else if (!activeDisk.endsWith("/")) {
                activeDisk += "/";
            }
            activeDisks.add(activeDisk);
        }
        return activeDisks;
    }

    /**
     * 是否启用文件管理功能
     * @return boolean
     */
    public static boolean enableFileManager() {
        String enabledStr = RealConfig.get("enabled-file-manager");
        return enabledStr != null && (enabledStr.equalsIgnoreCase("true") || enabledStr.equals("1"));
    }

    /**
     * 返回所有可操作的目录列表
     * @return 目录列表
     */
    public static List<StoreInfo> getAllDir() {
        List<StoreInfo> result = getReadDir();
        result.addAll(getWriteDir());
        return result.stream()
                .sorted((d1, d2) -> d1.getPath().compareToIgnoreCase(d2.getPath()))
                .toList();
    }


    /**
     * 指定的目录，是否是允许读操作的目录的子目录
     *
     * @param usedDir 要操作的子目录
     * @return 是否允许读取
     */
    public static boolean canReadDir(String usedDir) {
        if (!enableFileManager()) {
            return false;
        }
        for (StoreInfo dir : getAllDir()) {
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
    public static boolean canWriteDir(String usedDir) {
        if (!enableFileManager()) {
            return false;
        }
        for (StoreInfo dir : getAllDir()) {
            if (!dir.isReadonly() && usedDir.startsWith(dir.getPath())) {
                return true;
            }
        }
        return false;
    }

    /// 下面是私有方法

    /**
     * 返回允许操作的只读根目录列表
     *
     * @return 允许操作的根目录列表
     */
    private static List<StoreInfo> getReadDir() {
        if (!enableFileManager()) {
            return new ArrayList<>();
        }
        String readDirStr = RealConfig.get("read-dir");
        return toStoreInfoList(readDirStr, true);
    }

    /**
     * 返回允许操作的可读可写根目录列表
     *
     * @return 允许操作的根目录列表
     */
    private static List<StoreInfo> getWriteDir() {
        if (!enableFileManager()) {
            return new ArrayList<>();
        }
        String dirStr = RealConfig.get("write-dir");
        return toStoreInfoList(dirStr, false);
    }

    // 把逗号分隔的目录列表，转换为StoreInfo数组返回
    private static List<StoreInfo> toStoreInfoList(String dirStr, boolean readonly) {
        if (dirStr == null || dirStr.isEmpty()) {
            return null;
        }
        dirStr = dirStr.replace('\\', '/')
                .replace("//", "/");

        // 按逗号分隔目录
        return java.util.Arrays.stream(dirStr.split(","))
                .map(item -> convertToStoreInfo(item, readonly))
                .filter(item -> !item.getName().isEmpty() && new File(item.getName()).isDirectory())
                .collect(Collectors.toList());
    }

    private static StoreInfo convertToStoreInfo(String dir, boolean readonly) {
        dir = FileHelper.clearDirName(dir);
        return new StoreInfo()
                .setDir(true)
                .setReadonly(readonly)
                .setName(dir)
                .setPath(dir);
    }
}
