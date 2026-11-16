package cn.beinet.deployment.admin.autoConfig;

import cn.beinet.business.login.loginValidate.Validator;
import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.core.base.configs.ConfigConst;
import cn.beinet.core.base.consts.ContextConst;
import cn.beinet.core.base.exceptions.BaseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用于拦截所有的请求，并验证是否登录
 *
 * @author youbl
 * @since 2025/5/21 14:10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

    private final List<Validator> validatorList;
    private final ObjectMapper objectMapper;
    // private final AuditLogService auditLogService;

    private final static String DEFAULT_ALLOW_HEADERS = "Authorization,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,token,simple-auth";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 允许跨域的请求头添加
        addCorsHeaders(request, response);

        // OPTIONS请求直接返回
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        try {
            // 逐一调用验证器，比如不需要登录的页面、比如SDK登录等等
            for (Validator item : validatorList) {
                var result = item.validated(request, response);
                if (result.isPassed()) {
                    String account = result.getAccount();

                    // 添加登录后的信息
                    request.setAttribute(ContextConst.LOGIN_COOKIE_NAME, account);

                    if (result.getUserId() != null) {
                        request.setAttribute(ContextConst.LOGIN_USER_ID, result.getUserId());
                        MDC.put(ContextConst.LOGIN_USER_ID, result.getUserId().toString());
                    }

                    filterChain.doFilter(request, response);
                    return;
                }
            }
        } catch (BaseException e) {
            int code = e.getCode() > 0 ? e.getCode() : 403;
            endResponse(request, response, code, e.getMsg() != null ? e.getMsg() : e.getMessage());
            return;
        }

        if (ConfigConst.isDev()) {
            // 开发环境用的调试代码
            request.setAttribute(ContextConst.LOGIN_COOKIE_NAME, "Dev_Debug");
            filterChain.doFilter(request, response);
            return;
        }

        // 记录认证失败
        // auditLogService.recordAuthenticationFailed(request, "Token验证失败或不存在");

        // @ExceptionHandler(Exception.class) 不会拦截Filter里的异常，要自己返回
        endResponse(request, response, 401, "请重新登录: " + request.getRequestURI());
        //throw new BaseException(401, "请重新登录: " + request.getRequestURI());
    }


    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin)) {
            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            // Access-Control-Allow-Credentials不允许Access-Control-Allow-Origin使用 *
            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }
        response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS, PUT, DELETE");

        String allowHeader = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (StringUtils.hasLength(allowHeader)) {
            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeader);
        } else {
            response.addHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, DEFAULT_ALLOW_HEADERS);
        }
    }


    /**
     * 终止响应，并返回错误信息
     *
     * @param request  请求上下文
     * @param response 响应上下文
     * @param code     错误码
     * @param msg      错误信息
     */
    public void endResponse(HttpServletRequest request, HttpServletResponse response, int code, String msg) {
        if (shouldRedirectToLoginPage(request)) {
            redirectToLoginPage(request, response);
            return;
        }
        writeJson(response, code, msg, null);
    }

    /**
     * 浏览器地址栏/链接触发的文档 GET 请求未登录时跳转登录页；接口与 Ajax 仍返回 JSON。
     */
    private static boolean shouldRedirectToLoginPage(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String xrw = request.getHeader("X-Requested-With");
        if (StringUtils.hasText(xrw) && "XMLHttpRequest".equalsIgnoreCase(xrw.trim())) {
            return false;
        }
        String dest = request.getHeader("Sec-Fetch-Dest");
        if (StringUtils.hasText(dest) && "document".equalsIgnoreCase(dest.trim())) {
            return true;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.toLowerCase().contains("text/html");
    }

    private static void redirectToLoginPage(HttpServletRequest request, HttpServletResponse response) {
        String ctx = request.getContextPath();
        String returnUrl = buildAbsoluteRequestUrl(request);
        String target = ctx + "/login.html?url=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, response.encodeRedirectURL(target));
    }

    private static String buildAbsoluteRequestUrl(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String query = request.getQueryString();
        if (StringUtils.hasText(query)) {
            url.append('?').append(query);
        }
        return url.toString();
    }

    private void writeJson(HttpServletResponse response, int code, String msg, Object obj) {
        try {
            response.setStatus(HttpServletResponse.SC_OK);
            var data = ResponseData.fail(code, msg, obj);
            String json = objectMapper.writeValueAsString(data);
            response.setContentType("application/json; charset=UTF-8");
            response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exp) {
            log.error("登录出错", exp);
        }
    }
}
