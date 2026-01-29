package cn.beinet.business.login.health;

import cn.beinet.business.login.dal.UsersMapper;
import cn.beinet.business.login.dto.HealthCheckDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务健康检查器
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Component("authHealth")
@Slf4j
@RequiredArgsConstructor
public class AuthHealthIndicator implements HealthIndicator {

    private final UsersMapper usersMapper;
    //private final RedisTemplate<String, String> redisTemplate;

    @Override
    public Health health() {
        try {
            HealthCheckDto healthCheck = performHealthCheck();
            
            if ("UP".equals(healthCheck.getStatus())) {
                return Health.up()
                        .withDetail("checkTime", healthCheck.getCheckTime())
                        .withDetail("components", healthCheck.getComponents())
                        .withDetail("details", healthCheck.getDetails())
                        .build();
            } else {
                return Health.down()
                        .withDetail("checkTime", healthCheck.getCheckTime())
                        .withDetail("components", healthCheck.getComponents())
                        .withDetail("details", healthCheck.getDetails())
                        .build();
            }
        } catch (Exception e) {
            log.error("认证服务健康检查失败", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("checkTime", LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 执行健康检查
     * 
     * @return 健康检查结果
     */
    public HealthCheckDto performHealthCheck() {
        Map<String, HealthCheckDto.ComponentHealth> components = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        boolean allHealthy = true;

        // 检查数据库连接
        HealthCheckDto.ComponentHealth dbHealth = checkDatabase();
        components.put("database", dbHealth);
        if (!"UP".equals(dbHealth.getStatus())) {
            allHealthy = false;
        }

        // 检查Redis连接
//        HealthCheckDto.ComponentHealth redisHealth = checkRedis();
//        components.put("redis", redisHealth);
//        if (!"UP".equals(redisHealth.getStatus())) {
//            allHealthy = false;
//        }

        // 添加统计信息
        details.put("totalComponents", components.size());
        details.put("healthyComponents", components.values().stream()
                .mapToInt(c -> "UP".equals(c.getStatus()) ? 1 : 0)
                .sum());

        return HealthCheckDto.builder()
                .status(allHealthy ? "UP" : "DOWN")
                .checkTime(LocalDateTime.now())
                .components(components)
                .details(details)
                .build();
    }

    /**
     * 检查数据库连接
     */
    private HealthCheckDto.ComponentHealth checkDatabase() {
        long startTime = System.currentTimeMillis();
        try {
            // 执行简单查询测试数据库连接
            Long userCount = usersMapper.selectCount(null);
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> details = new HashMap<>();
            details.put("userCount", userCount);
            details.put("connectionTest", "SUCCESS");
            
            return HealthCheckDto.ComponentHealth.builder()
                    .status("UP")
                    .details(details)
                    .duration(duration)
                    .build();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("数据库健康检查失败", e);
            
            Map<String, Object> details = new HashMap<>();
            details.put("connectionTest", "FAILED");
            
            return HealthCheckDto.ComponentHealth.builder()
                    .status("DOWN")
                    .details(details)
                    .error(e.getMessage())
                    .duration(duration)
                    .build();
        }
    }

    /**
     * 检查Redis连接
     */
//    private HealthCheckDto.ComponentHealth checkRedis() {
//        long startTime = System.currentTimeMillis();
//        try {
//            // 执行ping测试Redis连接
//            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
//            long duration = System.currentTimeMillis() - startTime;
//
//            Map<String, Object> details = new HashMap<>();
//            details.put("ping", pong);
//            details.put("connectionTest", "SUCCESS");
//
//            return HealthCheckDto.ComponentHealth.builder()
//                    .status("UP")
//                    .details(details)
//                    .duration(duration)
//                    .build();
//        } catch (Exception e) {
//            long duration = System.currentTimeMillis() - startTime;
//            log.error("Redis健康检查失败", e);
//
//            Map<String, Object> details = new HashMap<>();
//            details.put("connectionTest", "FAILED");
//
//            return HealthCheckDto.ComponentHealth.builder()
//                    .status("DOWN")
//                    .details(details)
//                    .error(e.getMessage())
//                    .duration(duration)
//                    .build();
//        }
//    }

}