package cn.beinet.business.login.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 审计日志传输对象
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Data
@Accessors(chain = true)
public class AuditLogDto {
    
    private Long id;
    private Long userId;
    private String username;
    private String eventType;
    private String eventDescription;
    private String ipAddress;
    private String userAgent;
    private String requestUri;
    private String sessionId;
    private Integer result;
    private String errorMessage;
    private LocalDateTime createTime;

    /**
     * 创建审计日志构建器
     * 
     * @return 构建器实例
     */
    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    /**
     * 审计日志构建器
     */
    public static class AuditLogBuilder {
        private final AuditLogDto auditLog = new AuditLogDto();

        public AuditLogBuilder userId(Long userId) {
            auditLog.setUserId(userId);
            return this;
        }

        public AuditLogBuilder eventType(String eventType) {
            auditLog.setEventType(eventType);
            return this;
        }

        public AuditLogBuilder eventDescription(String eventDescription) {
            auditLog.setEventDescription(eventDescription);
            return this;
        }

        public AuditLogBuilder ipAddress(String ipAddress) {
            auditLog.setIpAddress(ipAddress);
            return this;
        }

        public AuditLogBuilder userAgent(String userAgent) {
            auditLog.setUserAgent(userAgent);
            return this;
        }

        public AuditLogBuilder requestUri(String requestUri) {
            auditLog.setRequestUri(requestUri);
            return this;
        }

        public AuditLogBuilder sessionId(String sessionId) {
            auditLog.setSessionId(sessionId);
            return this;
        }

        public AuditLogBuilder result(Integer result) {
            auditLog.setResult(result);
            return this;
        }

        public AuditLogBuilder success() {
            auditLog.setResult(1);
            return this;
        }

        public AuditLogBuilder failed() {
            auditLog.setResult(0);
            return this;
        }

        public AuditLogBuilder errorMessage(String errorMessage) {
            auditLog.setErrorMessage(errorMessage);
            return this;
        }

        public AuditLogDto build() {
            return auditLog;
        }
    }
}