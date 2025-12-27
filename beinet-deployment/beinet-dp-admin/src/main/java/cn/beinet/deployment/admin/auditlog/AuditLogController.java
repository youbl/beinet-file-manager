package cn.beinet.deployment.admin.auditlog;

// import cn.beinet.business.login.service.AuditLogService;
import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.auditlog.dto.AuditLogQueryDto;
import cn.beinet.deployment.admin.auditlog.dto.AuditLogStatisticsDto;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计日志管理控制器
 * 
 * @author youbl
 * @since 2024-12-20
 */
@RestController
@RequestMapping("/audit-logs")
// @RequiredArgsConstructor
@Slf4j
@Tag(name = "审计日志管理", description = "审计日志查询和统计接口")
public class AuditLogController {

    // private final AuditLogService auditLogService;

    /*
    @PostMapping("/query")
    @Operation(summary = "查询审计日志", description = "分页查询审计日志，支持多条件筛选")
    public ResponseData<IPage<Map<String, Object>>> queryAuditLogs(@RequestBody AuditLogQueryDto queryDto) {
        try {
            IPage<Map<String, Object>> result = auditLogService.queryAuditLogs(
                    queryDto.getPage(),
                    queryDto.getSize(),
                    queryDto.getUserId(),
                    queryDto.getEventType(),
                    queryDto.getResult(),
                    queryDto.getStartTime(),
                    queryDto.getEndTime(),
                    queryDto.getIpAddress()
            );
            
            return ResponseData.ok(result);
        } catch (Exception e) {
            log.error("查询审计日志失败", e);
            return ResponseData.fail(500, "查询审计日志失败: " + e.getMessage());
        }
    }
    */

    /*
    @GetMapping("/statistics")
    @Operation(summary = "获取审计日志统计", description = "获取审计日志的统计信息")
    public ResponseData<AuditLogStatisticsDto> getStatistics(
            @RequestParam(defaultValue = "30") int days) {
        try {
            // 获取总体统计
            Map<String, Object> overallData = auditLogService.getOverallStatistics(days);
            AuditLogStatisticsDto.OverallStats overallStats = AuditLogStatisticsDto.OverallStats.builder()
                    .totalEvents(getLongValue(overallData, "totalEvents"))
                    .successEvents(getLongValue(overallData, "successEvents"))
                    .failedEvents(getLongValue(overallData, "failedEvents"))
                    .successRate(getDoubleValue(overallData, "successRate"))
                    .activeUsers(getLongValue(overallData, "activeUsers"))
                    .uniqueIps(getLongValue(overallData, "uniqueIps"))
                    .build();

            // 获取事件类型统计
            List<Map<String, Object>> eventTypeData = auditLogService.getEventTypeStatistics(days);
            List<AuditLogStatisticsDto.EventTypeStats> eventTypeStats = eventTypeData.stream()
                    .map(data -> AuditLogStatisticsDto.EventTypeStats.builder()
                            .eventType(getStringValue(data, "eventType"))
                            .eventDescription(getStringValue(data, "eventDescription"))
                            .count(getLongValue(data, "count"))
                            .successCount(getLongValue(data, "successCount"))
                            .failedCount(getLongValue(data, "failedCount"))
                            .successRate(getDoubleValue(data, "successRate"))
                            .build())
                    .collect(Collectors.toList());

            // 获取每日统计
            List<Map<String, Object>> dailyData = auditLogService.getDailyStatistics(days);
            List<AuditLogStatisticsDto.DailyStats> dailyStats = dailyData.stream()
                    .map(data -> AuditLogStatisticsDto.DailyStats.builder()
                            .date(getStringValue(data, "date"))
                            .totalEvents(getLongValue(data, "totalEvents"))
                            .successEvents(getLongValue(data, "successEvents"))
                            .failedEvents(getLongValue(data, "failedEvents"))
                            .activeUsers(getLongValue(data, "activeUsers"))
                            .build())
                    .collect(Collectors.toList());

            AuditLogStatisticsDto statistics = AuditLogStatisticsDto.builder()
                    .overallStats(overallStats)
                    .eventTypeStats(eventTypeStats)
                    .dailyStats(dailyStats)
                    .build();

            return ResponseData.ok(statistics);
        } catch (Exception e) {
            log.error("获取审计日志统计失败", e);
            return ResponseData.fail(500, "获取审计日志统计失败: " + e.getMessage());
        }
    }
    */

    /*
    @PostMapping("/cleanup")
    @Operation(summary = "清理旧日志", description = "清理指定天数之前的审计日志")
    public ResponseData<Integer> cleanupOldLogs(@RequestParam int retentionDays) {
        try {
            if (retentionDays < 30) {
                return ResponseData.fail(400, "保留天数不能少于30天");
            }
            
            int deletedCount = auditLogService.cleanupOldLogs(retentionDays);
            return ResponseData.ok(deletedCount);
        } catch (Exception e) {
            log.error("清理旧日志失败", e);
            return ResponseData.fail(500, "清理旧日志失败: " + e.getMessage());
        }
    }
    */

    @GetMapping("/event-types")
    @Operation(summary = "获取事件类型列表", description = "获取所有可用的事件类型")
    public ResponseData<List<Map<String, String>>> getEventTypes() {
        try {
            List<Map<String, String>> eventTypes = List.of(
                    Map.of("code", "LOGIN", "description", "用户登录"),
                    Map.of("code", "LOGOUT", "description", "用户登出"),
                    Map.of("code", "AUTH_FAILED", "description", "认证失败"),
                    Map.of("code", "TOKEN_EXPIRED", "description", "Token过期"),
                    Map.of("code", "ACCESS_DENIED", "description", "访问被拒绝"),
                    Map.of("code", "ACCESS_SUCCESS", "description", "访问成功"),
                    Map.of("code", "GITHUB_AUTH_START", "description", "GitHub认证开始"),
                    Map.of("code", "GITHUB_AUTH_SUCCESS", "description", "GitHub认证成功"),
                    Map.of("code", "GITHUB_AUTH_FAILED", "description", "GitHub认证失败"),
                    Map.of("code", "USER_STATUS_CHANGE", "description", "用户状态变更"),
                    Map.of("code", "USER_DELETE", "description", "用户删除")
            );
            
            return ResponseData.ok(eventTypes);
        } catch (Exception e) {
            log.error("获取事件类型列表失败", e);
            return ResponseData.fail(500, "获取事件类型列表失败: " + e.getMessage());
        }
    }

    /**
     * 安全地获取Long值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 安全地获取Double值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 安全地获取String值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}