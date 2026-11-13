package cn.beinet.deployment.admin.auth;

import cn.beinet.core.base.commonDto.ResponseData;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 普通 MVC Controller：发起授权、处理回调、查看会话及退出；没有 SecurityFilterChain。 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final OAuthClients clients;
    private final SessionAuthorizationRequests requests;
    private final OAuthGateway gateway;
    private final OAuthUsers users;
    private final AuthProperties properties;

    @GetMapping("/auth/providers")
    public ResponseData<List<String>> providers() { return ResponseData.ok(clients.names()); }

    @GetMapping("/auth/session")
    public ResponseData<Map<String, Object>> session(HttpServletRequest request) {
        LoginUser user = AuthSupport.user(request); // AuthorizationFilter 已检查登录和账号状态。
        return ResponseData.ok(Map.of("id", user.id(), "email", user.email(), "provider", user.provider(),
                "admin", properties.isAdmin(user.email())));
    }

    @GetMapping("/oauth2/authorization/{provider}")
    public void start(@PathVariable String provider, @RequestParam(required = false) String returnTo,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            OAuthClients.Client client = clients.require(provider);
            var pending = requests.start(request, provider, returnTo);
            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(pending.verifier().getBytes(StandardCharsets.US_ASCII)));
            // state 绑定会话；PKCE verifier 留在服务端，只发送其 SHA-256 摘要。
            String url = UriComponentsBuilder.fromUriString(client.authorizationUri())
                    .queryParam("response_type", "code").queryParam("client_id", client.clientId())
                    .queryParam("redirect_uri", client.redirectUri()).queryParam("scope", client.scopes())
                    .queryParam("state", pending.state()).queryParam("code_challenge", challenge)
                    .queryParam("code_challenge_method", "S256").build().encode().toUriString();
            response.sendRedirect(url);
        } catch (Exception e) { failed(request, response, e); }
    }

    @GetMapping("/login/oauth2/code/{provider}")
    public void callback(@PathVariable String provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            OAuthClients.Client client = clients.require(provider);
            // 先验证并消费 state，再检查供应商错误或进行换票；不能让无效回调触发外部请求。
            var pending = requests.consume(request, provider, request.getParameter("state"));
            if (request.getParameter("error") != null) throw new LoginFailure("access_denied");
            String code = request.getParameter("code");
            if (code == null || code.isBlank() || code.length() > 8192) throw new LoginFailure("invalid_grant");
            String token = gateway.exchange(client, code, pending.verifier());
            Map<String, Object> profile = gateway.profile(client, token);
            // Google 采用 OAuth2 用户信息接口，不接收浏览器提供的 ID Token，也不自行解析 JWT。
            LoginUser user = "google".equals(provider) ? users.google(profile)
                    : users.github(profile, gateway.githubEmails(token));
            // 销毁旧会话并新建，防止 Session 固定攻击；供应商 Token 不存入会话或数据库。
            AuthSupport.clearSession(request);
            request.getSession(true).setAttribute(LoginUser.SESSION_KEY, user);
            AuthSupport.csrfCookie(request, response, properties.getBaseUrl().startsWith("https://"));
            log.info("OAuth login succeeded reference={} provider={} subject={} userId={} email={} name={} emailVerified=true allowed={} admin={} remoteAddress={} userAgent={} callback={} configuredOrigin={}",
                    UUID.randomUUID(), user.provider(), LoginDiagnostics.safe(user.subject()), user.id(),
                    LoginDiagnostics.safe(user.email()), LoginDiagnostics.safe(user.name()), properties.allows(user.email()),
                    properties.isAdmin(user.email()), LoginDiagnostics.safe(request.getRemoteAddr()),
                    LoginDiagnostics.safe(request.getHeader("User-Agent")), request.getServletPath(), properties.getBaseUrl());
            response.sendRedirect(pending.returnTo());
        } catch (RuntimeException e) { failed(request, response, e); }
    }

    private void failed(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        String reference = UUID.randomUUID().toString();
        String reason = LoginDiagnostics.reason(exception);
        log.warn("OAuth login failed reference={} reason={} callback={} configuredOrigin={} sessionPresent={} remoteAddress={} userAgent={} details={}",
                reference, reason, request.getServletPath(), properties.getBaseUrl(), request.getSession(false) != null,
                LoginDiagnostics.safe(request.getRemoteAddr()), LoginDiagnostics.safe(request.getHeader("User-Agent")),
                LoginDiagnostics.details(exception));
        AuthSupport.clearSession(request);
        response.sendRedirect("/login.html?error=login_failed&reason=" + reason + "&reference=" + reference);
    }

    @PostMapping("/auth/logout")
    public ResponseData<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        LoginUser user = AuthSupport.user(request); // 登录与CSRF已经由AuthorizationFilter校验。
        log.info("Local session logged out userId={} provider={}", user.id(), user.provider());
        AuthSupport.clearSession(request);
        for (String name : List.of("JSESSIONID", "beinet-user", "XSRF-TOKEN")) {
            response.addHeader("Set-Cookie", ResponseCookie.from(name, "").path("/").maxAge(0)
                    .httpOnly(!"XSRF-TOKEN".equals(name)).secure(properties.getBaseUrl().startsWith("https://"))
                    .sameSite("Lax").build().toString());
        }
        return ResponseData.ok(null);
    }
}
