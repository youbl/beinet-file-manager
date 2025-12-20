package cn.beinet.deployment.admin.autoConfig;

import cn.beinet.business.login.loginValidate.Validator;
import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.core.base.configs.ConfigConst;
import cn.beinet.core.base.consts.ContextConst;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

        // 逐一调用验证器，比如不需要登录的页面、比如SDK登录等等
        for (Validator item : validatorList) {
            var result = item.validated(request, response);
            if (result.isPassed()) {
                String account = result.getAccount();

                // 添加登录后的信息
                request.setAttribute(ContextConst.LOGIN_COOKIE_NAME, account);

                filterChain.doFilter(request, response);
                return;
            }
        }
        if (ConfigConst.isDev()) {
            // 开发环境用的调试代码
            request.setAttribute(ContextConst.LOGIN_COOKIE_NAME, "Dev_Debug");
            filterChain.doFilter(request, response);
            return;
        }

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
//        String redirectUrl = thirdLoginService.combineLoginUrl(request);
//        if (!isAjax(request)) {
//            redirect(response, redirectUrl);
//        } else {
        writeJson(response, code, msg, null);
//        }
    }

    private void writeJson(HttpServletResponse response, int code, String msg, Object obj) {
        try {
            var data = ResponseData.fail(code, msg, obj);
            String json = objectMapper.writeValueAsString(data);
            response.setContentType("application/json; charset=UTF-8");
            response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exp) {
            log.error("登录出错", exp);
        }
    }
}
