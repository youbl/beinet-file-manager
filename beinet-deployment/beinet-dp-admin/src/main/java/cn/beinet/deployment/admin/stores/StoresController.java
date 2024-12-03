package cn.beinet.deployment.admin.stores;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import cn.beinet.deployment.admin.stores.services.StoreService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseData<String> uploadFile(@RequestBody MultipartFile file,
                                           @RequestParam String dir) {
        if (file == null) {
            return ResponseData.fail(500, "未提交文件");
        }

        String fullName = storeService.uploadFile(file, dir);
        return ResponseData.ok(fullName);
    }

    @GetMapping("stores/list")
    public ResponseData<List<StoreInfo>> getList(@RequestParam(required = false) String prefix) {
        var ret = storeService.getList(prefix);
        return ResponseData.ok(ret);
    }

    @GetMapping("stores/download")
    public void download(@RequestParam String file, HttpServletResponse response) {
        storeService.download(file, response);
    }
}
