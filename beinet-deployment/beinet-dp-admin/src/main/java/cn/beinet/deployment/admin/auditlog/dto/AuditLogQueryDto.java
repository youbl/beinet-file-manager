package cn.beinet.deployment.admin.auditlog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志查询参数
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Data
@Schema(description = "审计日志查询参数")
public class AuditLogQueryDto {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "结果（1-成功，0-失败）")
    private Integer result;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "用户名或邮箱（搜索关键词）")
    private String keyword;
}