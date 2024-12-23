package cn.beinet.deployment.admin.stores;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.stores.dtos.StoreInfo;
import cn.beinet.deployment.admin.stores.services.SingleDirService;
import cn.beinet.deployment.admin.stores.services.StoreService;
import cn.beinet.deployment.admin.stores.services.UnRarService;
import cn.beinet.sdk.event.EventUtils;
import cn.beinet.sdk.event.enums.EventSubType;
import io.swagger.v3.oas.annotations.Hidden;
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

import java.io.File;
import java.util.List;

/**
 * 文件上传接口类
 *
 * @author youbl
 * @since 2024/12/3 16:23
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "files", description = "文件管理接口类")
public class StoresController {
    private final StoreService storeService;
    private final UnRarService unRarService;
    private final SingleDirService singleDirService;

    /**
     * 上传文件到指定的目录下
     *
     * @param file 文件
     * @param dir  目录
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
     *
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
     *
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
     *
     * @param file     要下载的全路径
     * @param response 响应上下文
     * @param request  请求上下文
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

    // 一次性工具，用于某个目录下的所有rar文件解压
    @GetMapping("stores/extractRar")
    @Hidden
    public ResponseData<String> extractRar(@RequestParam String dir, @RequestParam String pwd) {
        File directory = new File(dir);
        if (!directory.exists() || !directory.isDirectory()) {
            return ResponseData.ok("目录不存在:" + dir);
        }

        if (!unRarService.isCompleted()) {
            // 上次解压任务还没结束
            return getUnRarStatus();
        }

        unRarService.unrar(dir, pwd);
        return ResponseData.ok("解压任务已启动，请调用接口检查结果");
    }

    // 查询上面接口的解压状态
    @GetMapping("stores/extractRarStatus")
    @Hidden
    public ResponseData<String> getUnRarStatus() {
        String status = (unRarService.isCompleted()) ? "*******结束*******" : "进行中";
        String ret = "解压" + status +
                ": 成功 " + unRarService.getSuccessCount() +
                " 个, 失败 " + unRarService.getFailureCount() +
                " 个，耗时 " + unRarService.costTime() + " 毫秒";
        return ResponseData.ok(ret);
    }

    // 一次性工具，用于目录提级，如果某个目录下有且只有一个子目录，且没有文件，则把子目录里的内容提取到上级目录，并删除该子目录
    @GetMapping("stores/singleDir")
    @Hidden
    public ResponseData<String> singleDir(@RequestParam String dir) {
        File directory = new File(dir);
        if (!directory.exists() || !directory.isDirectory()) {
            return ResponseData.ok("目录不存在:" + dir);
        }

        if (!singleDirService.isCompleted()) {
            // 上次任务还没结束
            return singleDirStatus();
        }

        singleDirService.scan(dir);
        return ResponseData.ok("提级任务已启动，请调用接口检查结果");
    }

    // 查询上面接口的解压状态
    @GetMapping("stores/singleDirStatus")
    @Hidden
    public ResponseData<String> singleDirStatus() {
        String status = (singleDirService.isCompleted()) ? "*******结束*******" : "进行中";
        String ret = "扫描" + status +
                ": 已迁移 " + singleDirService.getSuccessCount() +
                " 个, 耗时 " + singleDirService.costTime() + " 毫秒";
        return ResponseData.ok(ret);
    }
}
