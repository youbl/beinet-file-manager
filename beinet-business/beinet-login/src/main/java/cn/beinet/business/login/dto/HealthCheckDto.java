package cn.beinet.business.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康检查状态
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Data
@Builder
@Schema(description = "健康检查状态")
public class HealthCheckDto {

    @Schema(description = "整体状态", example = "UP")
    private String status;

    @Schema(description = "检查时间")
    private LocalDateTime checkTime;

    @Schema(description = "各组件状态")
    private Map<String, ComponentHealth> components;

    @Schema(description = "详细信息")
    private Map<String, Object> details;

    @Data
    @Builder
    @Schema(description = "组件健康状态")
    public static class ComponentHealth {
        @Schema(description = "状态", example = "UP")
        private String status;

        @Schema(description = "详细信息")
        private Map<String, Object> details;

        @Schema(description = "错误信息")
        private String error;

        @Schema(description = "检查耗时（毫秒）")
        private Long duration;
    }
}