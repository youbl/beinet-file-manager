package cn.beinet.core.thirdparty.aliyun.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 安全组dto对象
 * @author youbl
 * @since 2025/4/27 10:24
 */
@Data
@Accessors(chain = true)
public class AliyunSecurityGroupDto {

    private String createTime;
    /**
     * 规则描述
     */
    private String description;
    private String destCidrIp;
    private String destGroupId;
    private String destGroupName;
    private String destGroupOwnerAccount;
    private String destPrefixListId;
    private String destPrefixListName;
    /**
     * 规则方向
     */
    private String direction;
    private String ipProtocol;
    private String ipv6DestCidrIp;
    private String ipv6SourceCidrIp;
    /**
     * 网卡类型
     */
    private String nicType;
    /**
     * 访问权限
     */
    private String policy;
    private String portRange;
    private String portRangeListId;
    private String portRangeListName;
    /**
     * 规则优先级
     */
    private String priority;
    /**
     * 规则id
     */
    private String securityGroupRuleId;
    /**
     * 源端IPv4 CIDR地址段
     */
    private String sourceCidrIp;
    private String sourceGroupId;
    private String sourceGroupName;
    private String sourceGroupOwnerAccount;
    /**
     * 端口范围
     */
    private String sourcePortRange;
    private String sourcePrefixListId;
    private String sourcePrefixListName;

}
