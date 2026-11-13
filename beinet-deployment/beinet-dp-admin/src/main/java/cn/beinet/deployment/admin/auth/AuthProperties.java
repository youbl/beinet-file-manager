package cn.beinet.deployment.admin.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 统一认证配置；有效登录名单由普通邮箱与管理员邮箱取并集，不回写 YML。 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    // 本站对外访问地址，对应 auth.base-url；用于拼接 OAuth 回调地址，不是供应商地址。
    private String baseUrl = "http://localhost:8080";
    // 普通登录邮箱名单，对应 auth.allowed-emails；实际准入还包括 adminEmails 中的邮箱。
    private List<String> allowedEmails = new ArrayList<>();
    // 管理员邮箱名单，对应 auth.admin-emails；匹配者同时拥有登录资格和管理员角色。
    private List<String> adminEmails = new ArrayList<>();
    // 允许携带登录 Cookie 的前端来源；必须填写完整协议、主机和端口，不能填写路径或 *。
    private Cors cors = new Cors();

    @Data
    public static class Cors {
        // 对应 auth.cors.allowed-origins；空列表表示不允许外部来源跨域访问。
        private List<String> allowedOrigins = new ArrayList<>();
    }

    /** 本站或明确配置的 HTTP(S) 来源才可信；拒绝 null 来源、通配符及含路径的地址。 */
    public boolean allowsOrigin(String origin) {
        if (origin == null || origin.equals("null")) {
            return false;
        }
        try {
            var uri = java.net.URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || !uri.getRawPath().isEmpty() || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                return false;
            }
        } catch (IllegalArgumentException exception) {
            return false;
        }
        return origin.equals(baseUrl.replaceAll("/+$", ""))
                || (cors != null && cors.allowedOrigins != null && cors.allowedOrigins.contains(origin));
    }

    private Provider google = new Provider();
    private Provider github = new Provider();

    /** 判断邮箱是否在普通名单或管理员名单中；任意一个匹配即返回 true。 */
    public boolean allows(String email) {
        // 管理员邮箱自动获得登录资格，无需在 allowed-emails 中重复填写。
        return contains(allowedEmails, email) || contains(adminEmails, email);
    }

    /** 判断邮箱是否属于配置的管理员名单；返回值不受数据库 users.isAdmin 字段影响。 */
    public boolean isAdmin(String email) {
        return allows(email) && contains(adminEmails, email);
    }

    // 精确匹配邮箱，仅忽略大小写和首尾空格；不支持域名通配符或邮箱别名归并。
    private boolean contains(List<String> entries, String email) {
        if (entries == null || email == null || email.isBlank()) {
            return false;
        }
        return entries.stream().filter(item -> item != null && !item.isBlank())
                .map(AuthProperties::normalize).anyMatch(normalize(email)::equals);
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Data
    public static class Provider {
        // 供应商 OAuth 应用的客户端 ID，对应 auth.google.client-id 或 auth.github.client-id。
        private String clientId = "";
        // 与客户端 ID 配套的密钥，仅供服务端换票使用，不返回给前端。
        private String clientSecret = "";

        // 两项均空表示停用该供应商；只填一项视为配置错误，阻止应用带着错误配置启动。
        public boolean enabled() {
            boolean id = clientId != null && !clientId.isBlank();
            boolean secret = clientSecret != null && !clientSecret.isBlank();
            if (id != secret) {
                throw new IllegalStateException("OAuth client ID and secret must both be configured");
            }
            return id;
        }
    }
}
