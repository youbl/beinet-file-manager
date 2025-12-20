package cn.beinet.deployment.admin.auditlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 审计日志统计结果
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Data
@Builder
@Schema(description = "审计日志统计结果")
public class AuditLogStatisticsDto {

    @Schema(description = "总体统计")
    private OverallStats overallStats;

    @Schema(description = "事件类型统计")
    private List<EventTypeStats> eventTypeStats;

    @Schema(description = "每日统计")
    private List<DailyStats> dailyStats;

    @Data
    @Builder
    @Schema(description = "总体统计")
    public static class OverallStats {
        @Schema(description = "总事件数")
        private Long totalEvents;

        @Schema(description = "成功事件数")
        private Long successEvents;

        @Schema(description = "失败事件数")
        private Long failedEvents;

        @Schema(description = "成功率")
        private Double successRate;

        @Schema(description = "活跃用户数")
        private Long activeUsers;

        @Schema(description = "独立IP数")
        private Long uniqueIps;
    }

    @Data
    @Builder
    @Schema(description = "事件类型统计")
    public static class EventTypeStats {
        @Schema(description = "事件类型")
        private String eventType;

        @Schema(description = "事件描述")
        private String eventDescription;

        @Schema(description = "事件数量")
        private Long count;

        @Schema(description = "成功数量")
        private Long successCount;

        @Schema(description = "失败数量")
        private Long failedCount;

        @Schema(description = "成功率")
        private Double successRate;
    }

    @Data
    @Builder
    @Schema(description = "每日统计")
    public static class DailyStats {
        @Schema(description = "日期")
        private String date;

        @Schema(description = "总事件数")
        private Long totalEvents;

        @Schema(description = "成功事件数")
        private Long successEvents;

        @Schema(description = "失败事件数")
        private Long failedEvents;

        @Schema(description = "活跃用户数")
        private Long activeUsers;
    }
}