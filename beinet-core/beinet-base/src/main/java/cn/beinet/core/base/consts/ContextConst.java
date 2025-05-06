package cn.beinet.core.base.consts;

public class ContextConst {
    public static final String HEADER_PREFIX = "x-";
    public static final String HEADER_PREFIX_ACCEPT = "accept-";
    public static final String HEADER_USER_ID = "x-user-id";
    public static final String HEADER_X_TRACE_ID = "x-trace-id";
    public static final String HEADER_ENCRYPT = "x-encrypt";
    public static final String HEADER_APPLICATION = "x-application-name";
    public static final String HEADER_PRODUCT = "product";
    public static final String HEADER_ACCEPT_LANGUAGE = "accept-language";
    /**
     * 收到请求的时间，可以便于后端计算响应时长
     */
    public static final String HEADER_REQUEST_TIME = "x-request-time";
}
