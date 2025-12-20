package cn.beinet.business.login.enums;

/**
 * 审计事件类型枚举
 * 
 * @author youbl
 * @since 2024-12-20
 */
public enum AuditEventType {

    /**
     * 用户登录
     */
    LOGIN("LOGIN", "用户登录"),

    /**
     * 用户登出
     */
    LOGOUT("LOGOUT", "用户登出"),

    /**
     * 认证失败
     */
    AUTH_FAILED("AUTH_FAILED", "认证失败"),

    /**
     * Token过期
     */
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token过期"),

    /**
     * 访问被拒绝
     */
    ACCESS_DENIED("ACCESS_DENIED", "访问被拒绝"),

    /**
     * 访问成功
     */
    ACCESS_SUCCESS("ACCESS_SUCCESS", "访问成功"),

    /**
     * GitHub认证开始
     */
    GITHUB_AUTH_START("GITHUB_AUTH_START", "GitHub认证开始"),

    /**
     * GitHub认证成功
     */
    GITHUB_AUTH_SUCCESS("GITHUB_AUTH_SUCCESS", "GitHub认证成功"),

    /**
     * GitHub认证失败
     */
    GITHUB_AUTH_FAILED("GITHUB_AUTH_FAILED", "GitHub认证失败");

    private final String code;
    private final String description;

    AuditEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举
     * 
     * @param code 事件代码
     * @return 对应的枚举值
     */
    public static AuditEventType fromCode(String code) {
        for (AuditEventType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown audit event type: " + code);
    }
}