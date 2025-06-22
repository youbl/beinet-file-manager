package cn.beinet.deployment.admin.aliyun;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.core.thirdparty.aliyun.AliyunSecurityGroupUtils;
import cn.beinet.core.thirdparty.aliyun.dto.AliyunSecurityGroupDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 阿里云相关接口
 * @author youbl
 * @since 2025/4/27 11:50
 */
@RestController
@RequiredArgsConstructor
public class AliyunController {
    private final AliyunSecurityGroupUtils aliyunSecurityGroupUtils;

    @PostMapping("api/aliyun/security-group")
    public ResponseData<List<AliyunSecurityGroupDto>> getSecurityGroups(@RequestParam String groupId) {
        return ResponseData.ok(aliyunSecurityGroupUtils.getSecurityGroups(groupId));
    }

    @DeleteMapping("api/aliyun/security-group")
    public ResponseData<Void> delSecurityGroups(@RequestParam String groupId, @RequestParam String ruleId) {
        aliyunSecurityGroupUtils.delSecurityGroup(groupId, ruleId);
        return ResponseData.ok();
    }

    @PostMapping("api/aliyun/security-group/status/enable")
    public ResponseData<Void> enableSecurityGroups(@RequestParam String groupId, @RequestParam String ruleId) {
        aliyunSecurityGroupUtils.changeSecurityGroupStatus(groupId, ruleId, true);
        return ResponseData.ok();
    }

    @PostMapping("api/aliyun/security-group/status/disable")
    public ResponseData<Void> disableSecurityGroups(@RequestParam String groupId, @RequestParam String ruleId) {
        aliyunSecurityGroupUtils.changeSecurityGroupStatus(groupId, ruleId, false);
        return ResponseData.ok();
    }
}
