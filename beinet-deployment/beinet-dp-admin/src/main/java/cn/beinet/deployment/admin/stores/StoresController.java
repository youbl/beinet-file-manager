package cn.beinet.deployment.admin.stores;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import cn.beinet.deployment.admin.stores.services.StoreService;
import cn.beinet.sdk.event.EventUtils;
import cn.beinet.sdk.event.enums.EventSubType;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传接口类
 * @author youbl
 * @since 2024/12/3 16:23
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "files", description = "文件上传接口类")
public class StoresController {
    private final StoreService storeService;

    /**
     * 上传文件到指定的目录下
     * @param file 文件
     * @param dir 目录
     * @return 上传结果文件全路径
     */
    @PostMapping("stores/uploadFile")
    //@EventLog(subType = EventSubType.FILE_UPLOAD)
    public ResponseData<String> uploadFile(@RequestBody MultipartFile file, @RequestParam String dir) {
        if (file == null) {
            return ResponseData.fail(500, "未提交文件");
        }

        log.info("准备上传:{} {}", dir, file.getOriginalFilename());
        String fullName = storeService.uploadFile(file, dir);
        // EventLog注解会忽略file，导致日志无法记录实际上传结果，因此不使用注解，改自行代码上报
        EventUtils.report(EventSubType.FILE_UPLOAD, fullName);
        return ResponseData.ok(fullName);
    }

    /**
     * 获取指定目录的所有子目录和一级文件
     * @param dir 目录，为空时返回配置的目录列表
     * @return 所有子目录和一级文件
     */
    @GetMapping("stores/list")
    public ResponseData<List<StoreInfo>> getList(@RequestParam(required = false) String dir) {
        var ret = storeService.getList(dir);
        return ResponseData.ok(ret);
    }

    /**
     * 获取指定目录的状态，如是否允许上传
     * @param dir 目录
     * @return 状态
     */
    @GetMapping("stores/status")
    public ResponseData<StoreInfo> getStatus(@RequestParam String dir) {
        var ret = storeService.getStatus(dir);
        return ResponseData.ok(ret);
    }

    /**
     * 下载指定文件
     * @param file 要下载的全路径
     * @param response 响应上下文
     * @param request 请求上下文
     */
    @GetMapping("stores/download")
    public void download(@RequestParam String file, HttpServletResponse response, HttpServletRequest request) {
        String rangeHeader = request.getHeader("Range");
        if (rangeHeader != null) {
            // 存在Range头，说明是分片请求
            storeService.downloadWithRange(file, response, rangeHeader);
        } else {
            // 普通下载
            storeService.download(file, response);
        }
    }
}
