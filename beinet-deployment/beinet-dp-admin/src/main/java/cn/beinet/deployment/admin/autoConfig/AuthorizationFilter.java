package cn.beinet.deployment.admin.autoConfig;

import cn.beinet.business.login.service.OAuthAccountService;
import cn.beinet.core.base.consts.ContextConst;
import cn.beinet.deployment.admin.auth.AuthProperties;
import cn.beinet.deployment.admin.auth.LoginUser;
import cn.beinet.deployment.admin.auth.AuthSupport;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 统一业务访问控制入口。按 doFilterInternal 中的顺序阅读即可看到所有放行和拒绝条件。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthorizationFilter extends OncePerRequestFilter {
    // 允许匿名公开访问的 URL 路由集合，使用完整路径精确匹配，不包含通配符。
    // isPublic() 仅对 GET/HEAD 请求使用此集合，并额外允许 /res/ 开头的静态资源路径。
    // 业务 HTML、文件下载、Swagger 和健康检查不在此列表，默认都需要登录。
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/login.html",
            "/auth/providers",
            "/favicon.ico",
            "/oauth2/authorization/google",
            "/oauth2/authorization/github",
            "/login/oauth2/code/google",
            "/login/oauth2/code/github"
    );

    // 已停用接口的完整路径；即使管理员登录也不能访问。
    private static final Set<String> DISABLED_PATHS = Set.of(
            "/login/github",
            "/login/google",
            "/github/callback",
            "/google/token",
            "/tools/ymldecrypt",
            "/stores/extractRar",
            "/stores/singleDir");

    // 需要管理员权限的路径根节点。匹配根节点本身或其 / 子路径，不匹配 /users-other 等相似名字。
    private static final List<String> ADMIN_ROOTS = List.of(
            "/users", "/api/aliyun", "/aliyun", "/aws", "/tools", "/config", "/audit-logs");

    private final AuthProperties properties;
    private final OAuthAccountService accounts;

    public AuthorizationFilter(AuthProperties properties, OAuthAccountService accounts) {
        this.properties = properties;
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        addSafeHeader(response);

        String rawPath = request.getRequestURI();
        // 拒绝存在路径歧义的输入，避免容器解码/路径参数处理与权限判断不一致。
        if (rawPath.contains("%") ||
                rawPath.contains(";") ||
                rawPath.contains("\\") ||
                rawPath.contains("//") ||
                rawPath.contains("/../") ||
                rawPath.contains("/./") ||
                rawPath.endsWith("/..") ||
                rawPath.endsWith("/.") ||
                rawPath.chars().anyMatch(Character::isISOControl)) {
            AuthSupport.error(response, 400, "Invalid request path");
            return;
        }

        // 不允许的HTTP METHOD
        if ("TRACE".equals(request.getMethod()) || "CONNECT".equals(request.getMethod())) {
            AuthSupport.error(response, 405, "Method not allowed");
            return;
        }

        // 允许的HTTP METHOD
        if ("OPTIONS".equals(request.getMethod())) {
            AuthSupport.ok(response, null);
            return;
        }

        // 1. 公开路径允许匿名访问；为登录页初始化CSRF，静态资源不创建会话。
        if (isPublic(request)) {
            if ("/login.html".equals(rawPath)) {
                // 登录页面添加一个csrf的cookie
                AuthSupport.csrfCookie(request, response, properties.getBaseUrl().startsWith("https://"));
            }
            chain.doFilter(request, response);
            return;
        }

        // 2. 登录身份只来自服务端 Session；不读取客户端用户 Header、JWT 或自行填写的用户 Cookie。
        LoginUser user = AuthSupport.user(request);
        if (user == null) {
            AuthSupport.unauthorized(request, response);
            return;
        }
        String email = user.email();
        long userId = user.id();

        // 3. 管理员自动纳入有效名单；名单撤销后销毁会话。
        if (userId <= 0 || !properties.allows(email)) {
            AuthSupport.clearSession(request);
            AuthSupport.unauthorized(request, response);
            return;
        }
        // 所有写请求（包括退出）比较 Session CSRF 值；有 Origin 时还必须属于本站或跨域白名单。
        boolean readOnly = "GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod());
        if (!readOnly) {
            String origin = request.getHeader("Origin");
            if (!AuthSupport.validCsrf(request) || (origin != null && !properties.allowsOrigin(origin))) {
                AuthSupport.error(response, 403, "Invalid CSRF token or origin");
                return;
            }
        }

        // 4. 每次业务访问都查本地账号，禁用或软删除立即阻止后续访问。
        boolean enabled;
        try {
            enabled = accounts.isEnabled(userId);
        } catch (RuntimeException exception) {
            // 查库失败不等于账号被禁用：保留 Session 便于恢复后重试，但本次业务返回 503。
            AuthSupport.error(response, 503, "Account verification unavailable");
            return;
        }

        if (!enabled) {
            AuthSupport.clearSession(request);
            AuthSupport.unauthorized(request, response);
            return;
        }

        String path = request.getServletPath();
        if (path.isEmpty()) {
            path = request.getRequestURI();
        }

        // 5. 停用入口始终拒绝。先检查此项，避免管理员通过 /tools 管理权限访问危险工具。
        if (DISABLED_PATHS.contains(path) || isPathOrChild(path, "/demo")) {
            AuthSupport.error(response, 403, "Access denied");
            return;
        }

        // 6. 管理路径只允许配置中的管理员；普通业务路径只需满足前面的登录与账号检查。
        boolean admin = properties.isAdmin(email);
        if (isAdminPath(path) && !admin) {
            AuthSupport.error(response, 403, "Administrator required");
            return;
        }

        // 7. 请求属性供业务读取；管理员判断使用配置，不再构造框架角色对象。
        request.setAttribute(ContextConst.LOGIN_COOKIE_NAME, email);
        request.setAttribute(ContextConst.HEADER_USER_ID, Long.toString(userId));
        AuthSupport.csrfCookie(request, response, properties.getBaseUrl().startsWith("https://"));

        // 8. 只有前面的检查全部通过，才允许进入 Controller 或业务 HTML/下载资源处理。
        chain.doFilter(request, response);
    }

    private void addSafeHeader(HttpServletResponse response) {
        // 0. 统一安全响应头；不将动态身份/业务响应缓存到共享缓存。
        // 禁止浏览器mime嗅探，严格按服务端返回的`Content-Type` 解析，不自动猜类型
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 禁止跨域名 iframe 嵌入本页面，只有同域名允许
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        // 跨域跳转不发送Referer；同域跳转允许完整 Referer
        response.setHeader("Referrer-Policy", "same-origin");
        // 禁止浏览器、中间代理缓存
        response.setHeader("Cache-Control", "no-store");
    }

    private static boolean isAdminPath(String path) {
        for (String root : ADMIN_ROOTS) {
            if (isPathOrChild(path, root)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPathOrChild(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    /**
     * 判断请求是否允许匿名访问：方法为 GET/HEAD，且路径在 PUBLIC_PATHS 中或以 /res/ 开头时返回 true。
     */
    static boolean isPublic(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.HEAD.matches(request.getMethod())) {
            return false;
        }
        String path = request.getServletPath();
        if (path.isEmpty()) {
            path = request.getRequestURI();
        }
        return PUBLIC_PATHS.contains(path) || path.startsWith("/res/");
    }

}
