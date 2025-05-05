package cn.beinet.core.thirdparty.aliyun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云相关配置
 * @author youbl
 * @since 2025/4/27 16:04
 */
@Data
@Component
@ConfigurationProperties(prefix = "ali")
public class AliyunConfig {
    private AliyunSecurityPolicy securityPolicy;

    @Data
    public static class AliyunSecurityPolicy {
        private String regionId;
        private String accessKeyId;
        private String accessKeySecret;
        private String groupId;
    }
}
