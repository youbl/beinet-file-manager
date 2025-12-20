package cn.beinet.business.login.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 白名单配置类
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth.whitelist")
public class WhitelistConfig {

    /**
     * 是否启用白名单检查
     */
    private boolean enabled = true;

    /**
     * 白名单路径列表
     */
    private List<String> paths = Arrays.asList(
        "/login/**",
        "/swagger-ui/**", 
        "/v3/api-docs/**",
        "/static/**",
        "/favicon.ico",
        "/error",
        "/actuator/health"
    );

    /**
     * 检查路径是否在白名单中
     * 
     * @param requestUri 请求URI
     * @return 是否在白名单中
     */
    public boolean isWhitelisted(String requestUri) {
        if (!enabled || requestUri == null) {
            return false;
        }

        return paths.stream().anyMatch(pattern -> {
            // 支持 Ant 风格的路径匹配
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                return requestUri.startsWith(prefix);
            } else if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                return requestUri.startsWith(prefix) && 
                       requestUri.indexOf('/', prefix.length()) == -1;
            } else {
                return requestUri.equals(pattern);
            }
        });
    }
}