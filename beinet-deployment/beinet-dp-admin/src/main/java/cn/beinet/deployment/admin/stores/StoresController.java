package cn.beinet.deployment.admin.stores;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import cn.beinet.deployment.admin.stores.services.StoreService;
import cn.beinet.sdk.event.annotation.EventLog;
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

    @PostMapping("stores/uploadFile")
    @EventLog(subType = EventSubType.FILE_UPLOAD)
    public ResponseData<String> uploadFile(@RequestBody MultipartFile file,
                                           @RequestParam String dir) {
        if (file == null) {
            return ResponseData.fail(500, "未提交文件");
        }

        String fullName = storeService.uploadFile(file, dir);
        return ResponseData.ok(fullName);
    }

    @GetMapping("stores/list")
    public ResponseData<List<StoreInfo>> getList(@RequestParam(required = false) String dir) {
        var ret = storeService.getList(dir);
        return ResponseData.ok(ret);
    }

    @GetMapping("stores/status")
    public ResponseData<StoreInfo> getStatus(@RequestParam String dir) {
        var ret = storeService.getStatus(dir);
        return ResponseData.ok(ret);
    }

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
