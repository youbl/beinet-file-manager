package cn.beinet.core.aliyun;

import cn.beinet.core.aliyun.config.AliyunConfig;
import cn.beinet.core.aliyun.dto.AliyunSecurityGroupDto;
import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupAttributeRequest;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupAttributeResponse;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupAttributeResponseBody;
import com.aliyun.ecs20140526.models.ModifySecurityGroupRuleRequest;
import com.aliyun.ecs20140526.models.ModifySecurityGroupRuleResponse;
import com.aliyun.ecs20140526.models.RevokeSecurityGroupRequest;
import com.aliyun.ecs20140526.models.RevokeSecurityGroupResponse;
import com.aliyun.teaopenapi.models.Config;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云安全组辅助类
 * @author youbl
 * @since 2025/4/27 10:19
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AliyunSecurityGroupUtils {
    private final AliyunConfig aliConfig;

    private Client client;

    /**
     * 查询指定安全组的组内规则信息.
     * 参考文档：<a href="https://next.api.aliyun.com/document/Ecs/2014-05-26/DescribeSecurityGroupAttribute">...</a>
     * @param groupId 安全组id
     * @return 规则信息
     */
    @SneakyThrows
    public List<AliyunSecurityGroupDto> getSecurityGroups(String groupId) {
        groupId = StringUtils.hasText(groupId) ? groupId.trim() : aliConfig.getSecurityPolicy().getGroupId();

        DescribeSecurityGroupAttributeRequest req = new DescribeSecurityGroupAttributeRequest();
        req.regionId = aliConfig.getSecurityPolicy().getRegionId();
        req.securityGroupId = groupId;
        req.direction = "all";
        DescribeSecurityGroupAttributeResponse securityGroupResponse = getClient().describeSecurityGroupAttribute(req);
        DescribeSecurityGroupAttributeResponseBody detail = securityGroupResponse.body;
        log.info("安全组 {}({}) 规则条数: {}", detail.securityGroupName, detail.securityGroupId, detail.permissions.permission.size());

        List<AliyunSecurityGroupDto> ret = new ArrayList<>(detail.permissions.permission.size());
        for (DescribeSecurityGroupAttributeResponseBody.DescribeSecurityGroupAttributeResponseBodyPermissionsPermission permission : detail.permissions.permission) {
            AliyunSecurityGroupDto dto = new AliyunSecurityGroupDto();
            BeanUtils.copyProperties(permission, dto);
            ret.add(dto);
        }
        return ret;
    }

    /**
     * 删除指定安全组内的某条规则。
     * 参考文档: <a href="https://next.api.aliyun.com/document/Ecs/2014-05-26/RevokeSecurityGroup">...</a>
     * @param groupId 安全组id
     * @param ruleId 组内的规则id
     */
    @SneakyThrows
    public void delSecurityGroup(String groupId, final String ruleId) {
        groupId = StringUtils.hasText(groupId) ? groupId.trim() : aliConfig.getSecurityPolicy().getGroupId();

        RevokeSecurityGroupRequest request = new RevokeSecurityGroupRequest();
        request.regionId = aliConfig.getSecurityPolicy().getRegionId();
        request.securityGroupId = groupId;
        request.securityGroupRuleId = new ArrayList<>();
        request.securityGroupRuleId.add(ruleId);
        RevokeSecurityGroupResponse response = getClient().revokeSecurityGroup(request);
        log.info("安全组:{} 规则:{} 删除请求id:{}", groupId, ruleId, response.body.requestId);
    }

    /**
     * 允许或拒绝指定安全组内的某条规则。
     * 参考文档: <a href="https://next.api.aliyun.com/document/Ecs/2014-05-26/ModifySecurityGroupRule">...</a>
     * @param groupId 安全组id
     * @param ruleId 组内的规则id
     * @param status true表示允许规则，false表示拒绝规则
     */
    @SneakyThrows
    public void changeSecurityGroupStatus(String groupId, final String ruleId, final boolean status) {
        String policy = status ? "accept" : "drop";
        groupId = StringUtils.hasText(groupId) ? groupId.trim() : aliConfig.getSecurityPolicy().getGroupId();

        ModifySecurityGroupRuleRequest request = new ModifySecurityGroupRuleRequest();
        request.regionId = aliConfig.getSecurityPolicy().getRegionId();
        request.securityGroupId = groupId;
        request.securityGroupRuleId = ruleId;
        request.setPolicy(policy);
        ModifySecurityGroupRuleResponse response = getClient().modifySecurityGroupRule(request);
        log.info("安全组:{} 规则:{} 状态改为:{} 修改请求id:{}", groupId, ruleId, policy, response.body.requestId);
    }


    @SneakyThrows
    private Client getClient() {
        if (client == null) {
            Config config = new Config();
            // 您的AccessKey ID
            config.accessKeyId = aliConfig.getSecurityPolicy().getAccessKeyId();
            // 您的AccessKey Secret
            config.accessKeySecret = aliConfig.getSecurityPolicy().getAccessKeySecret();
            // 您的可用区ID，这个接口可以查可用区列表：https://help.aliyun.com/zh/ecs/developer-reference/api-ecs-2014-05-26-describeregions?spm=api-workbench.API%20Document.0.0.42266a3f00JPaM
            config.regionId = aliConfig.getSecurityPolicy().getRegionId();
            client = new com.aliyun.ecs20140526.Client(config);
        }
        return client;
    }
}
