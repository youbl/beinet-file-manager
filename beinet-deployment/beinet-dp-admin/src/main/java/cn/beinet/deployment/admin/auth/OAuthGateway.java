package cn.beinet.deployment.admin.auth;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** 只负责固定供应商端点的 HTTP 调用，不处理本站权限；测试可替换此对象，不访问真实供应商。 */
@Component
public class OAuthGateway {
    private final RestClient http;
    public OAuthGateway() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        http = RestClient.builder().requestFactory(factory).defaultHeader("User-Agent", "Beinet-File-Manager").build();
    }
    // 测试注入本地HTTP模拟客户端；生产默认构造器始终使用固定端点和超时。
    OAuthGateway(RestClient http) { this.http = http; }

    public String exchange(OAuthClients.Client client, String code, String verifier) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", client.clientId());
        form.add("client_secret", client.clientSecret());
        form.add("redirect_uri", client.redirectUri());
        form.add("code", code);
        form.add("code_verifier", verifier);
        try {
            Map<String, Object> body = http.post().uri(client.tokenUri()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON).body(form).retrieve().body(new ParameterizedTypeReference<>() {});
            if (body == null || body.containsKey("error") || !(body.get("access_token") instanceof String token)
                    || token.isBlank() || !(body.get("token_type") instanceof String type) || !"bearer".equalsIgnoreCase(type)) {
                throw new LoginFailure("invalid_token_response");
            }
            return token;
        } catch (LoginFailure e) { throw e; }
        catch (RuntimeException e) { throw new LoginFailure("invalid_token_response", "provider=" + client.id(), e); }
    }
    public Map<String, Object> profile(OAuthClients.Client client, String token) {
        try {
            Map<String, Object> result = http.get().uri(client.userInfoUri()).headers(h -> h.setBearerAuth(token))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
            if (result == null || result.containsKey("error")) throw new LoginFailure("invalid_user_info_response");
            return result;
        } catch (LoginFailure e) { throw e; }
        catch (RuntimeException e) { throw new LoginFailure("invalid_user_info_response", "provider=" + client.id(), e); }
    }
    public List<Map<String, Object>> githubEmails(String token) {
        try {
            return http.get().uri("https://api.github.com/user/emails?per_page=100").headers(h -> h.setBearerAuth(token))
                    .retrieve().body(new ParameterizedTypeReference<>() {});
        } catch (RuntimeException e) { throw new LoginFailure("github_email_fetch_failed", "provider=github", e); }
    }
}
