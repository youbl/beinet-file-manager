package cn.beinet.deployment.admin.auth;

import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 两个供应商的固定端点与配置；禁止通过请求参数改变换票地址或回调站点。 */
@Component
public class OAuthClients {
    private final Map<String, Client> clients = new LinkedHashMap<>();
    public OAuthClients(AuthProperties properties) {
        URI origin = URI.create(properties.getBaseUrl());
        boolean local = "http".equals(origin.getScheme())
                && ("localhost".equals(origin.getHost()) || "127.0.0.1".equals(origin.getHost()));
        if ((!"https".equals(origin.getScheme()) && !local) || origin.getHost() == null
                || origin.getUserInfo() != null || origin.getQuery() != null || origin.getFragment() != null
                || (origin.getPath() != null && !origin.getPath().isEmpty() && !origin.getPath().equals("/"))) {
            throw new IllegalStateException("auth.base-url must be an HTTPS origin (HTTP loopback allowed)");
        }
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        add("google", properties.getGoogle(), base, "https://accounts.google.com/o/oauth2/v2/auth",
                "https://oauth2.googleapis.com/token", "https://www.googleapis.com/oauth2/v2/userinfo",
                "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
        add("github", properties.getGithub(), base, "https://github.com/login/oauth/authorize",
                "https://github.com/login/oauth/access_token", "https://api.github.com/user", "read:user user:email");
    }
    private void add(String id, AuthProperties.Provider config, String base, String authorization, String token,
                     String userInfo, String scopes) {
        if (config.enabled()) clients.put(id, new Client(id, config.getClientId(), config.getClientSecret(),
                base + "/login/oauth2/code/" + id, authorization, token, userInfo, scopes));
    }
    public List<String> names() { return List.copyOf(clients.keySet()); }
    public Client require(String provider) {
        Client value = clients.get(provider);
        if (value == null) throw new LoginFailure("provider_not_configured");
        return value;
    }
    // 普通配置对象，不记录到日志，避免将客户端密钥打印出来。
    public record Client(String id, String clientId, String clientSecret, String redirectUri,
                         String authorizationUri, String tokenUri, String userInfoUri, String scopes) {
        @Override public String toString() { return "OAuthClient[" + id + "]"; }
    }
}
