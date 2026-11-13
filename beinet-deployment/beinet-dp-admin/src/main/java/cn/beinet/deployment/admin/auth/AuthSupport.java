package cn.beinet.deployment.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 会话、CSRF、回跳与错误响应的普通辅助方法，不依赖安全框架。
 */
public final class AuthSupport {
    private static final SecureRandom RANDOM = new SecureRandom();
    // Session 中保存 CSRF 随机值的 key；浏览器 Cookie 只用于让前端读取并回传。
    private static final String CSRF_KEY = AuthSupport.class.getName() + ".csrf";

    private AuthSupport() {
    }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static boolean same(String expected, String actual) {
        return expected != null && actual != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public static LoginUser user(HttpServletRequest request) {
        var session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(LoginUser.SESSION_KEY);
        return value instanceof LoginUser user ? user : null;
    }

    public static void clearSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
    }

    public static void csrfCookie(HttpServletRequest request, HttpServletResponse response, boolean secure) {
        var session = request.getSession();
        String token;
        synchronized (session) {
            token = (String) session.getAttribute(CSRF_KEY);
            if (token == null) {
                token = randomToken();
                session.setAttribute(CSRF_KEY, token);
            }
        }
        // 跨域前端无法读取 API 域名的 Cookie，可从允许暴露的响应头取得 CSRF 值。
        response.setHeader("X-XSRF-TOKEN", token);
        response.addHeader("Set-Cookie", ResponseCookie.from("XSRF-TOKEN", token)
                .path("/").httpOnly(false).secure(secure).sameSite("Lax").build().toString());
    }

    /**
     * 与服务端 Session 内的随机值比较，不信任浏览器自行提供的 Cookie 值。
     */
    public static boolean validCsrf(HttpServletRequest request) {
        var session = request.getSession(false);
        String expected = session == null ? null : (String) session.getAttribute(CSRF_KEY);
        return same(expected, request.getHeader("X-XSRF-TOKEN"));
    }

    public static void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI();
        if (("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
                && (path.equals("/") || path.endsWith(".html"))) {
            String query = request.getQueryString();
            response.sendRedirect("/login.html?returnTo=" + URLEncoder.encode(
                    safeReturnTo(path + (query == null ? "" : "?" + query)), StandardCharsets.UTF_8));
        } else error(response, 401, "Login required");
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        // message 仅由本项目固定文案提供，不传入外部异常或用户输入。
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + message + "\",\"data\":null}");
    }

    public static void ok(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        if (StringUtils.hasLength(message)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Cache-Control", "no-store");
            // message 仅由本项目固定文案提供，不传入外部异常或用户输入。
            response.getWriter().write("{\"code\":0,\"msg\":\"" + message + "\",\"data\":null}");
        }
    }

    /**
     * 合法本站回跳原样返回，否则进入默认文件管理页面。
     */
    public static String safeReturnTo(String target) {
        if (target != null && target.startsWith("/") && !target.startsWith("//") && target.length() <= 2048
                && !target.contains("\\") && target.chars().noneMatch(Character::isISOControl)) {
            try {
                URI uri = URI.create(target);
                String path = uri.getRawPath();
                if (!uri.isAbsolute() && uri.getRawAuthority() == null && path != null
                        && (path.equals("/") || path.matches("/[a-zA-Z0-9/_-]+\\.html"))
                        && !path.equals("/login.html") && !path.startsWith("/demo/")) return target;
            } catch (IllegalArgumentException ignored) {
            }
        }
        return "/stores/store.html";
    }
}
