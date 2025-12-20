package cn.beinet.business.login;

import cn.beinet.business.login.dto.HealthCheckDto;
import cn.beinet.business.login.health.AuthHealthIndicator;
import cn.beinet.core.base.commonDto.ResponseData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 * 
 * @author youbl
 * @since 2024-12-20
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "健康检查", description = "认证服务健康检查接口")
public class HealthController {

    private final AuthHealthIndicator authHealthIndicator;

    @GetMapping("/auth")
    @Operation(summary = "认证服务健康检查", description = "检查认证服务各组件的健康状态")
    public ResponseData<HealthCheckDto> checkAuthHealth() {
        try {
            HealthCheckDto healthCheck = authHealthIndicator.performHealthCheck();
            
            if ("UP".equals(healthCheck.getStatus())) {
                return ResponseData.ok(healthCheck);
            } else {
                return ResponseData.fail(503, "认证服务不健康", healthCheck);
            }
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return ResponseData.fail(500, "健康检查失败: " + e.getMessage());
        }
    }
}