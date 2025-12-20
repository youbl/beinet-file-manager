package cn.beinet.deployment.admin.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 白名单配置传输对象
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Data
@Schema(description = "白名单配置")
public class WhitelistConfigDto {

    @Schema(description = "白名单路径列表", example = "['/public/**', '/api/health', '/login/**']")
    private List<String> paths;

    @Schema(description = "配置描述")
    private String description;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;
}