package cn.beinet.deployment.admin.stores.services;

import cn.beinet.core.base.exceptions.BaseException;
import cn.beinet.core.utils.FileHelper;
import cn.beinet.deployment.admin.stores.configs.FileManagerConfig;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import cn.beinet.deployment.admin.stores.enums.StoreErrorCode;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件存储服务类
 * @author youbl
 * @since 2024/12/3 17:25
 */
@Service
@RequiredArgsConstructor
public class StoreService {
    private final FileManagerConfig fileManagerConfig;

    public List<StoreInfo> getList(String dir) {
        if (dir == null || dir.isEmpty()) {
            return getConfigDir();
        }

        dir = validDir(dir);
        List<StoreInfo> storeInfos = new ArrayList<>();
        File dirFile = new File(dir);
        if (dirFile.isDirectory()) {
            File[] files = dirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    StoreInfo storeInfo = new StoreInfo()
                            .setName(file.getName())
                            .setPath(file.getAbsolutePath())
                            .setDir(file.isDirectory())
                            .setModified(file.lastModified())
                            .setSize(0);
                    if (!file.isDirectory()) {
                        storeInfo.setSize(file.length());
                    }
                    storeInfos.add(storeInfo);
                }
            }
        }

        // 先按目录排序，再按文件名排序
        storeInfos.sort((a, b) -> {
            int dirCompare = Boolean.compare(b.isDir(), a.isDir());
            if (dirCompare == 0) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
            return dirCompare;
        });
        return storeInfos;
    }

    /**
     * 上传文件到指定目录
     * @return 上传结果文件完整路径
     */
    public String uploadFile(MultipartFile file, String dir) {
        dir = validDir(dir);

        String fileName = StringUtils.hasLength(file.getOriginalFilename()) ? file.getOriginalFilename() : "noName";
        String fullName = dir + fileName;
        FileHelper.saveFile(fullName, file, false);

        return fullName;
    }

    @SneakyThrows
    public void download(String file, HttpServletResponse response) {
        String filePath = validDir(file);
        File fileToDownload = new File(filePath);
        if (!fileToDownload.exists() || !fileToDownload.isFile()) {
            throw BaseException.of(StoreErrorCode.STORE_ERR_FILE_NOT_EXISTS, "要下载的文件不存在:" + filePath);
        }

        response.setContentType("application/octet-stream");
        String encodedFileName = URLEncoder.encode(fileToDownload.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
        try (FileInputStream in = new FileInputStream(fileToDownload)) {
            ServletOutputStream out = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    /**
     * 校验目录是否允许操作
     * @param dir 要校验的子目录
     */
    private String validDir(String dir) {
        if (fileManagerConfig == null) {
            throw BaseException.of(StoreErrorCode.STORE_ERR_NO_BASE_DIR_CONFIG, "yml里没有配置可访问的目录");
        }
        String checkedDir = FileHelper.clearDirName(dir);
        if (!fileManagerConfig.containsDir(checkedDir)) {
            throw BaseException.of(StoreErrorCode.STORE_ERR_NO_PERMISSION, "不允许访问的目录:" + checkedDir);
        }
        return checkedDir;
    }

    private List<StoreInfo> getConfigDir() {
        var arr = fileManagerConfig.getDir();
        var ret = new ArrayList<StoreInfo>();
        for (var file : arr) {
            StoreInfo storeInfo = new StoreInfo()
                    .setName(file)
                    .setDir(true)
                    .setPath(file);
            ret.add(storeInfo);
        }
        return ret;
    }
}
