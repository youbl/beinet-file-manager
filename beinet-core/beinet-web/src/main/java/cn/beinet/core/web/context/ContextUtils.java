package cn.beinet.core.web.context;

import cn.beinet.core.base.configs.ConfigConst;
import cn.beinet.core.base.consts.LogConst;
import cn.beinet.core.utils.IpHelper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Duration;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import static cn.beinet.core.base.consts.ContextConst.HEADER_APPLICATION;
import static cn.beinet.core.base.consts.ContextConst.HEADER_PREFIX;
import static cn.beinet.core.base.consts.ContextConst.HEADER_PREFIX_ACCEPT;
import static cn.beinet.core.base.consts.ContextConst.HEADER_PRODUCT;
import static cn.beinet.core.base.consts.ContextConst.HEADER_REQUEST_TIME;
import static cn.beinet.core.base.consts.ContextConst.HEADER_USER_ID;
import static cn.beinet.core.base.consts.ContextConst.HEADER_X_TRACE_ID;


@Slf4j
public class ContextUtils {

    public static final String DEFAULT_PRODUCT = "beinet";

    /**
     * 从header里获取当前登录用户的用户id（前端写入）
     * @return 用户id
     */
    public static Long getUserId() {
        return getLongHeader(HEADER_USER_ID);
    }

    /**
     * 从header里获取当前访问的产品名称（前端写入），无数据时返回默认产品名
     * @return 产品名称
     */
    public static String getProduct() {
        String product = getHeader(HEADER_PRODUCT);
        return StringUtils.hasLength(product) ? product : DEFAULT_PRODUCT;
    }

    /**
     * 设置用户id到当前请求上下文属性里
     */
    public static void setUserId(Long userId) {
        setAttribute(HEADER_USER_ID, userId.toString());
    }

    /**
     * 从header里获取当前访问的跟踪id（前端写入）
     * @return 跟踪id
     */
    public static String getTraceId() {
        return getHeader(HEADER_X_TRACE_ID);
    }

    /**
     * 设置跟踪id到当前请求上下文属性里
     */
    public static void setTraceId(String traceId) {
        MDC.put(LogConst.TRACE_ID, traceId);
        setAttribute(HEADER_X_TRACE_ID, traceId);
    }

    /**
     * 按优先级顺序获取用户IP
     *
     * @return 单个IP
     */
    public static String getIp() {
        try {
            HttpServletRequest request = getRequest();
            return request != null ? IpHelper.getIpAddr(request) : null;
        } catch (Exception exp) {
            log.error("取IP出错", exp);
            return "";
        }
    }

    /**
     * 返回完整用户IP，包括所有header
     *
     * @return 所有IP
     */
    public static String getFullIp() {
        try {
            HttpServletRequest request = getRequest();
            return request != null ? IpHelper.getClientIp(request) : null;
        } catch (Exception exp) {
            log.error("getFullIpErr", exp);
            return "";
        }
    }

    /**
     * 获取当前请求url
     *
     * @param getMethod  是否拼接METHOD
     * @param getReferer 是否拼接Referer
     * @return 请求url
     */
    public static String getRequestUrl(boolean getMethod, boolean getReferer) {
        try {
            HttpServletRequest request = getRequest();
            if (request != null) {
                String para = request.getQueryString();
                if (StringUtils.hasLength(para))
                    para = "?" + para;
                else
                    para = "";

                String method = getMethod ? request.getMethod() + " " : "";
                String referer = getReferer ? " refer:" + request.getHeader("referer") : "";
                return method + request.getRequestURL() + para + referer;
            }
        } catch (Exception exp) {
            log.error("取url出错", exp);
        }
        return "";
    }

    /**
     * 设置当前时间戳到header中
     */
    public static void setRequestTime() {
        long timestamp = System.currentTimeMillis();
        setAttribute(HEADER_REQUEST_TIME, String.valueOf(timestamp));
    }

    /**
     * 获取header里的请求时间戳
     *
     * @return 请求时间戳
     */
    public static long getRequestTime() {
        Long result = getLongHeader(HEADER_REQUEST_TIME);
        return result == null ? 0 : result;
    }


    /**
     * 透传头部信息
     *
     * @return 获取需要透传的header
     */
    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        try {
            ServletRequestAttributes servletRequestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (servletRequestAttributes != null) {
                // 透传x-开头的所有头部信息
                HttpServletRequest request = servletRequestAttributes.getRequest();
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames != null && headerNames.hasMoreElements()) {
                    try {
                        String key = headerNames.nextElement();
                        if (isTransfer(key)) {
                            headers.put(key, request.getHeader(key));
                        }
                    } catch (Exception e) {
                        // headerNames.nextElement() 可能出现空指针
                        log.warn("获取header某个key异常", e);
                    }
                }
                try {
                    // 提取attributes
                    String[] attributeNames =
                            servletRequestAttributes.getAttributeNames(ServletRequestAttributes.SCOPE_REQUEST);
                    for (String name : attributeNames) {
                        if (StringUtils.hasLength(name) && !headers.containsKey(name) && isTransfer(name)) {
                            Object val =
                                    servletRequestAttributes.getAttribute(name, ServletRequestAttributes.SCOPE_REQUEST);
                            headers.put(name, (String) val);
                        }
                    }
                } catch (IllegalStateException illExp) {
                    // Async执行feign时，servletRequestAttributes.getAttributeNames会抛异常
                    log.error("取header.getAttributeNames异常", illExp);
                }
            }
        } catch (Exception exp) {
            log.error("取header异常:", exp);
        }
        // 如果没有traceId自动生成
        if (!headers.containsKey(HEADER_X_TRACE_ID)) {
            headers.put(HEADER_X_TRACE_ID, UUID.randomUUID().toString());
        }
        // 设置当前服务名
        headers.put(HEADER_APPLICATION, ConfigConst.getAppName());
        return headers;
    }

    public static Map<String, String> getAllHeaders() {
        Map<String, String> ret = new HashMap<>();
        try {
            ServletRequestAttributes servletRequestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (servletRequestAttributes != null) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                ret.put("", request.getMethod() + " " + request.getRequestURL());
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames != null && headerNames.hasMoreElements()) {
                    String key = headerNames.nextElement();

                    StringBuilder allVal = new StringBuilder();
                    Enumeration<String> headerVals = request.getHeaders(key);
                    while (headerVals != null && headerVals.hasMoreElements()) {
                        if (!allVal.isEmpty())
                            allVal.append(",");
                        allVal.append(headerVals.nextElement());
                    }
                    ret.put(key, allVal.toString());
                }
            }
        } catch (Exception exp) {
            log.error("getAllHeadersErr", exp);
        }
        return ret;
    }

    /**
     * 判断key是否透传
     *
     * @param key 键
     * @return 是否透传
     */
    public static boolean isTransfer(String key) {
        return key != null &&
                (key.startsWith(HEADER_PREFIX) ||
                        key.startsWith(HEADER_PREFIX_ACCEPT) ||
                        key.equalsIgnoreCase(HEADER_X_TRACE_ID) ||
                        key.equalsIgnoreCase(HEADER_PRODUCT));
    }

    /**
     * 从请求上下文中获取指定的cookie值
     * @param name cookie名
     * @return cookie值
     */
    public static String getCookie(String name) {
        if (!StringUtils.hasLength(name))
            return "";

        HttpServletRequest request = getRequest();
        if (request == null) {
            return "";
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return "";
        for (Cookie cookie : cookies) {
            if (name.equalsIgnoreCase(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return "";
    }

    /**
     * 在当前域名下添加Cookie
     * @param cookieName cookie名
     * @param value cookie值
     * @param expireSecond 过期时间，秒
     */
    public static void addCookie(String cookieName, String value, long expireSecond) {
        addCookie(cookieName, value, expireSecond, null);
    }

    /**
     * 在当前域名的根域名下添加Cookie
     * @param cookieName cookie名
     * @param value cookie值
     * @param expireSecond 过期时间，秒
     */
    public static void addCookieToBaseDomain(String cookieName, String value, long expireSecond) {
        String domain = getBaseDomain();         // 设置为二级域名用，便于跨域统一登录
        addCookie(cookieName, value, expireSecond, domain);
    }

    /**
     * 添加Cookie
     * @param cookieName cookie名
     * @param value cookie值
     * @param expireSecond 过期时间，秒
     * @param domain cookie所属域名
     */
    public static void addCookie(String cookieName, String value, long expireSecond, String domain) {
        // sameSite让跨域设置Cookie生效，必须搭配https，即secure(true)
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, value)
                .path("/")
                .maxAge(Duration.ofSeconds(expireSecond))
                .sameSite("None")  // 或 "Lax"、"Strict"
                .secure(true)      // SameSite=None 必须 Secure
                .httpOnly(true);

        if (StringUtils.hasLength(domain)) {
            builder.domain(domain);
        }
        ResponseCookie cookie = builder.build();
        var response = getResponse();
        if (response != null) {
            response.addHeader("Set-Cookie", cookie.toString());
        }
    }

    public static void clearHeaders() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 先从请求上下文，获取 request.setAttribute 设置的属性；
     * 不存在时，从请求的headers中获取
     * @param key 属性key
     * @return 属性在上下文里的值
     */
    public static String getHeader(String key) {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null) {
            return null;
        }
        try {
            String value = (String) servletRequestAttributes.getAttribute(key, ServletRequestAttributes.SCOPE_REQUEST);
            if (value == null) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                value = request.getHeader(key);
            }
            return value;
        } catch (IllegalStateException illExp) {
            // Async执行时，servletRequestAttributes.getAttribute会抛异常
            // j.l.IllegalStateException: Cannot ask for request attribute - request is not active anymore!
            return "";
        }
//        HttpServletRequest request = servletRequestAttributes.getRequest();
//        String value = request.getHeader(key);
//        if (value == null) {
//            value = (String) servletRequestAttributes.getAttribute(key, ServletRequestAttributes.SCOPE_REQUEST);
//        }
    }

    /**
     * 从请求header里获取Long类型值
     * @param key header key
     * @return header val
     */
    public static Long getLongHeader(String key) {
        String value = getHeader(key);
        if (value != null) {
            return Long.valueOf(value);
        }
        return null;
    }

    public static void setAttribute(String key, String value) {
        if (ConfigConst.isJobApp()) {
            throw new RuntimeException("Job不允许设置上下文，请检查代码，并修改实现方案");
        }
        init(null).setAttribute(key, value, ServletRequestAttributes.SCOPE_REQUEST);
    }

    public static RequestAttributes reset(Map<String, String> initialise) {
        RequestContextHolder.setRequestAttributes(null);
        return init(initialise);
    }

    public static RequestAttributes init(Map<String, String> initialise) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (Objects.nonNull(attributes)) {
            return attributes;
        }
        attributes = new ServletWebRequest(new CustomRequest(new HashMap<>()));
        RequestContextHolder.setRequestAttributes(attributes);
        if (Objects.isNull(initialise)) {
            return attributes;
        }
        // 复制初始值
        for (var key : initialise.keySet()) {
            attributes.setAttribute(key, initialise.get(key), ServletRequestAttributes.SCOPE_REQUEST);
        }
        return attributes;
    }

    /**
     * 从请求上下文中提取二级域名，如
     * www.abc.com 返回 abc.com
     * abc.def.ghi.xxx 返回 ghi.xxx
     *
     * @return 二级域名
     */
    public static String getBaseDomain() {
        var request = getRequest();
        if (request == null) {
            return "";
        }
        String domain = request.getServerName();
        // IP直接返回
        if (Pattern.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$", domain))
            return domain;
        return domain.replaceAll(".*\\.(?=.*\\.)", "");
    }

    public static HttpServletRequest getRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return servletRequestAttributes == null ? null : servletRequestAttributes.getRequest();
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return servletRequestAttributes == null ? null : servletRequestAttributes.getResponse();
    }
}
