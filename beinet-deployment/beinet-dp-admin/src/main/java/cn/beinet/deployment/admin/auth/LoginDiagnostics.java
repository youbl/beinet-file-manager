package cn.beinet.deployment.admin.auth;


import java.util.Set;

/** 输出受控错误分类与必要身份诊断，不输出供应商原始响应、认证凭据或 SQL 参数。 */
final class LoginDiagnostics {
    // 允许写入登录失败页 reason 参数的错误码集合；不在集合中的错误码映射为 unexpected_login_error。
    private static final Set<String> REASONS = Set.of("authorization_request_not_found", "invalid_state_parameter",
            "access_denied", "invalid_token_response", "invalid_user_info_response", "invalid_id_token",
            "invalid_nonce", "invalid_client", "invalid_grant", "email_not_verified", "email_not_allowed",
            "verified_email_not_allowed", "github_email_fetch_failed", "account_disabled", "account_storage_failed",
            "login_denied", "provider_not_configured");

    static String reason(Throwable error) {
        if (error instanceof LoginFailure failure && REASONS.contains(failure.reason())) return failure.reason();
        return "unexpected_login_error";
    }

    /** 日志字段的控制字符清理和长度限制入口，防止外部字段破坏日志结构。 */
    static String safe(Object value) {
        if (value == null) return "unavailable";
        String text = String.valueOf(value).replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", " ");
        return text.length() > 1024 ? text.substring(0, 1024) + "[truncated]" : text;
    }

    static String details(Throwable error) {
        StringBuilder result = new StringBuilder();
        // 防止异常 cause 循环，最多遍历十层；不直接输出可能含 Token 或 SQL 值的异常 message。
        Set<Throwable> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause) && seen.size() <= 10; cause = cause.getCause()) {
            result.append("\nCause: ").append(cause.getClass().getName());
            if (cause instanceof LoginFailure failure) result.append(" context={").append(failure.context()).append("}");
            if (cause instanceof LoginFailure failure) result.append(" reason=").append(reason(failure));
            if (cause instanceof java.sql.SQLException sql) {
                result.append(" SQLState=").append(safe(sql.getSQLState())).append(" vendorCode=").append(sql.getErrorCode());
            }
            if (cause instanceof org.springframework.web.client.RestClientResponseException http) {
                result.append(" HTTP status=").append(http.getStatusCode().value());
            }
            for (StackTraceElement frame : cause.getStackTrace()) result.append("\n  at ").append(frame);
        }
        return result.toString();
    }
}
