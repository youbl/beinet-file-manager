package cn.beinet.business.login.service;

import cn.beinet.business.login.dal.entity.AuditLogs;
import cn.beinet.business.login.dal.mapper.AuditLogsMapper;
import cn.beinet.business.login.dto.AuditLogDto;
import cn.beinet.business.login.enums.AuditEventType;
import cn.beinet.core.web.context.ContextUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 审计日志服务
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogsMapper auditLogsMapper;

    /**
     * 异步记录审计日志
     * 
     * @param auditLogDto 审计日志信息
     */
    @Async
    public void recordAuditLog(AuditLogDto auditLogDto) {
        try {
            AuditLogs auditLog = convertToEntity(auditLogDto);
            auditLogsMapper.insert(auditLog);
            log.debug("审计日志记录成功: {}", auditLogDto.getEventType());
        } catch (Exception e) {
            log.error("记录审计日志失败", e);
        }
    }

    /**
     * 记录登录成功事件
     * 
     * @param userId 用户ID
     * @param request HTTP请求
     */
    public void recordLoginSuccess(Long userId, HttpServletRequest request) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .userId(userId)
                .eventType(AuditEventType.LOGIN.getCode())
                .eventDescription("用户登录成功")
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .success()
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录登录失败事件
     * 
     * @param request HTTP请求
     * @param errorMessage 错误信息
     */
    public void recordLoginFailed(HttpServletRequest request, String errorMessage) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .eventType(AuditEventType.AUTH_FAILED.getCode())
                .eventDescription("用户登录失败")
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .failed()
                .errorMessage(errorMessage)
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录登出事件
     * 
     * @param userId 用户ID
     * @param request HTTP请求
     */
    public void recordLogout(Long userId, HttpServletRequest request) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .userId(userId)
                .eventType(AuditEventType.LOGOUT.getCode())
                .eventDescription("用户登出")
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .success()
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录GitHub认证开始事件
     * 
     * @param request HTTP请求
     */
    public void recordGithubAuthStart(HttpServletRequest request) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .eventType(AuditEventType.GITHUB_AUTH_START.getCode())
                .eventDescription("GitHub OAuth 认证开始")
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .success()
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录GitHub认证成功事件
     * 
     * @param userId 用户ID
     * @param request HTTP请求
     */
    public void recordGithubAuthSuccess(Long userId, HttpServletRequest request) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .userId(userId)
                .eventType(AuditEventType.GITHUB_AUTH_SUCCESS.getCode())
                .eventDescription("GitHub OAuth 认证成功")
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .success()
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录GitHub认证失败事件
     * 
     * @param request HTTP请求
     * @param errorMessage 错误信息
     */
    public void recordGithubAuthFailed(HttpServletRequest request, String errorMessage) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .eventType(AuditEventType.GITHUB_AUTH_FAILED.getCode())
                .eventDescription("GitHub OAuth 认证失败")
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .failed()
                .errorMessage(errorMessage)
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录访问成功事件
     * 
     * @param account 账号
     * @param request HTTP请求
     */
    public void recordAccessSuccess(String account, HttpServletRequest request) {
        // 只记录非匿名用户的访问
        if ("匿名".equals(account)) {
            return;
        }
        
        AuditLogDto auditLog = AuditLogDto.builder()
                .eventType(AuditEventType.ACCESS_SUCCESS.getCode())
                .eventDescription("访问成功: " + request.getRequestURI())
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .success()
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录认证失败事件
     * 
     * @param request HTTP请求
     * @param reason 失败原因
     */
    public void recordAuthenticationFailed(HttpServletRequest request, String reason) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .eventType(AuditEventType.AUTH_FAILED.getCode())
                .eventDescription("认证失败: " + reason)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .failed()
                .errorMessage(reason)
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 记录访问被拒绝事件
     * 
     * @param request HTTP请求
     * @param reason 拒绝原因
     */
    public void recordAccessDenied(HttpServletRequest request, String reason) {
        AuditLogDto auditLog = AuditLogDto.builder()
                .eventType(AuditEventType.ACCESS_DENIED.getCode())
                .eventDescription("访问被拒绝: " + reason)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .sessionId(getSessionId(request))
                .failed()
                .errorMessage(reason)
                .build();
        
        recordAuditLog(auditLog);
    }

    /**
     * 分页查询审计日志
     * 
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID（可选）
     * @param eventType 事件类型（可选）
     * @param result 结果（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param ipAddress IP地址（可选）
     * @return 分页结果
     */
    public IPage<Map<String, Object>> queryAuditLogs(int page, int size, Long userId, 
                                                     String eventType, Integer result,
                                                     LocalDateTime startTime, LocalDateTime endTime,
                                                     String ipAddress) {
        Page<Map<String, Object>> pageParam = new Page<>(page, size);
        return auditLogsMapper.selectAuditLogsWithUsername(pageParam, userId, eventType, 
                                                          result, startTime, endTime, ipAddress);
    }

    /**
     * 获取事件类型统计
     * 
     * @param days 统计天数
     * @return 统计结果
     */
    public List<Map<String, Object>> getEventTypeStatistics(int days) {
        return auditLogsMapper.selectEventTypeStatistics(days);
    }

    /**
     * 获取每日统计
     * 
     * @param days 统计天数
     * @return 统计结果
     */
    public List<Map<String, Object>> getDailyStatistics(int days) {
        return auditLogsMapper.selectDailyStatistics(days);
    }

    /**
     * 获取总体统计
     * 
     * @param days 统计天数
     * @return 统计结果
     */
    public Map<String, Object> getOverallStatistics(int days) {
        return auditLogsMapper.selectOverallStatistics(days);
    }

    /**
     * 清理旧的审计日志
     * 
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    public int cleanupOldLogs(int retentionDays) {
        int deletedCount = auditLogsMapper.cleanupOldLogs(retentionDays);
        log.info("清理了 {} 条超过 {} 天的审计日志", deletedCount, retentionDays);
        return deletedCount;
    }

    /**
     * 转换DTO为实体
     * 
     * @param dto 传输对象
     * @return 实体对象
     */
    private AuditLogs convertToEntity(AuditLogDto dto) {
        return new AuditLogs()
                .setUserId(dto.getUserId())
                .setEventType(dto.getEventType())
                .setEventDescription(dto.getEventDescription())
                .setIpAddress(dto.getIpAddress())
                .setUserAgent(dto.getUserAgent())
                .setRequestUri(dto.getRequestUri())
                .setSessionId(dto.getSessionId())
                .setResult(dto.getResult())
                .setErrorMessage(dto.getErrorMessage());
    }

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 获取会话ID
     * 
     * @param request HTTP请求
     * @return 会话ID
     */
    private String getSessionId(HttpServletRequest request) {
        try {
            return request.getSession(false) != null ? 
                   request.getSession().getId() : 
                   UUID.randomUUID().toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}