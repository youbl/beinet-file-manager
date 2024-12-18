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

    /**
     * 获取指定目录下的一级子目录和文件列表
     * @param dir 目录
     * @return 一级子目录和文件列表
     */
    public List<StoreInfo> getList(String dir) {
        if (dir == null || dir.isEmpty()) {
            return getConfigDir();
        }

        File dirFile = getReadableDir(dir);

        List<StoreInfo> storeInfos = new ArrayList<>();
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
                    } else {
                        storeInfo.setReadonly(isWritableDir(storeInfo.getPath()));
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
     * 获取指定目录的信息，包括是否允许上传
     * @param dir 目录
     * @return 目录信息
     */
    public StoreInfo getStatus(String dir) {
        dir = FileHelper.clearDirName(dir);
        if (dir.isEmpty() || !fileManagerConfig.canReadDir(dir)) {
            return null;
        }
        File dirFile = new File(dir);
        if (!dirFile.isDirectory()) {
            return null;
        }
        StoreInfo ret = new StoreInfo()
                .setName(dirFile.getName())
                .setPath(dirFile.getAbsolutePath())
                .setDir(true)
                .setModified(dirFile.lastModified())
                .setSize(0);
        boolean readonly = !isWritableDir(ret.getPath());
        ret.setReadonly(readonly);
        return ret;
    }

    /**
     * 上传文件到指定目录
     * @return 上传结果文件完整路径
     */
    public String uploadFile(MultipartFile file, String dir) {
        dir = FileHelper.clearDirName(dir);
        if (!fileManagerConfig.canWriteDir(dir)) {
            throw BaseException.of(StoreErrorCode.STORE_ERR_NO_PERMISSION, "不允许写入的目录:" + dir);
        }

        String fileName = StringUtils.hasLength(file.getOriginalFilename()) ? file.getOriginalFilename() : "noName";
        String fullName = dir + fileName;
        FileHelper.saveFile(fullName, file, false);

        return fullName;
    }

    @SneakyThrows
    public void download(String file, HttpServletResponse response) {
        File fileToDownload = getReadableDir(file);
        if (!fileToDownload.exists() || !fileToDownload.isFile()) {
            throw BaseException.of(StoreErrorCode.STORE_ERR_FILE_NOT_EXISTS, "要下载的文件不存在:" + file);
        }
        long fileLength = fileToDownload.length();
        response.setHeader("Content-Length", String.valueOf(fileLength));

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

    @SneakyThrows
    public void downloadWithRange(String file, HttpServletResponse response, String rangeHeader) {
        File fileToDownload = getReadableDir(file);
        if (!fileToDownload.exists() || !fileToDownload.isFile()) {
            throw BaseException.of(StoreErrorCode.STORE_ERR_FILE_NOT_EXISTS, "要下载的文件不存在:" + file);
        }

        long fileLength = fileToDownload.length();
        long start = 0;
        long end = fileLength - 1;

        // 解析Range头: bytes=start-end
        if (rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            try {
                if (ranges.length > 0 && !ranges[0].isEmpty()) {
                    start = Long.parseLong(ranges[0]);
                }
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException e) {
                throw BaseException.of(StoreErrorCode.STORE_ERR_RANGE_INVALID, "Range格式错误:" + rangeHeader);
            }
        }

        // 验证范围的有效性
        if (start < 0 || end >= fileLength || start > end) {
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            return;
        }

        // 计算本次需要传输的字节数
        long contentLength = end - start + 1;

        // 设置响应头
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 206
        response.setContentType(getContentType(file));
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Range", String.format("bytes %d-%d/%d", start, end, fileLength));
        response.setHeader("Content-Length", String.valueOf(contentLength));

        // 如果是视频文件，不设置下载头，允许浏览器直接播放
        if (!isVideoFile(file)) {
            String encodedFileName = URLEncoder.encode(fileToDownload.getName(), StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
        }

        // 发送指定范围的文件内容
        try (FileInputStream in = new FileInputStream(fileToDownload)) {
            in.skip(start);
            ServletOutputStream out = response.getOutputStream();
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            int length;

            while (remaining > 0 && (length = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                out.write(buffer, 0, length);
                remaining -= length;
            }
            out.flush();
        }
    }

    /**
     * 获取文件的Content-Type
     */
    private String getContentType(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".webm")) return "video/webm";
        if (fileName.endsWith(".ogg")) return "video/ogg";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        if (fileName.endsWith(".wav")) return "audio/wav";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }

    /**
     * 判断是否为视频文件
     */
    private boolean isVideoFile(String fileName) {
        fileName = fileName.toLowerCase();
        return fileName.endsWith(".mp4")
                || fileName.endsWith(".webm")
                || fileName.endsWith(".ogg")
                || fileName.endsWith(".mov")
                || fileName.endsWith(".avi")
                || fileName.endsWith(".mkv");
    }

    private boolean isWritableDir(String dir) {
        String checkedDir = FileHelper.clearDirName(dir);
        return (fileManagerConfig.canWriteDir(checkedDir));
    }

    private File getReadableDir(String dir) {
        dir = FileHelper.clearDirName(dir);
        if (!fileManagerConfig.canReadDir(dir)) {
            throw BaseException.of(StoreErrorCode.STORE_ERR_NO_PERMISSION, "不允许访问的目录:" + dir);
        }
        return new File(dir);
    }

    private List<StoreInfo> getConfigDir() {
        return fileManagerConfig.getDir();
    }
}
