package cn.beinet.deployment.admin.config;

// import cn.beinet.business.login.config.WhitelistConfig;
import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.config.dto.WhitelistConfigDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置管理控制器
 * 
 * @author youbl
 * @since 2024-12-20
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "系统配置管理", description = "系统配置查询和更新接口")
public class ConfigController {

    // private final WhitelistConfig whitelistConfig;
    private final ConfigurableEnvironment environment;

    /*
    @GetMapping("/whitelist")
    @Operation(summary = "获取白名单配置", description = "获取当前的白名单路径配置")
    public ResponseData<WhitelistConfigDto> getWhitelistConfig() {
        try {
            WhitelistConfigDto dto = new WhitelistConfigDto();
            dto.setPaths(whitelistConfig.getPaths());
            dto.setEnabled(whitelistConfig.getEnabled());
            dto.setDescription("无需登录认证的URL路径列表，支持Ant风格的路径匹配模式");
            
            return ResponseData.ok(dto);
        } catch (Exception e) {
            log.error("获取白名单配置失败", e);
            return ResponseData.fail(500, "获取白名单配置失败: " + e.getMessage());
        }
    }
    */

    /*
    @PutMapping("/whitelist")
    @Operation(summary = "更新白名单配置", description = "更新白名单路径配置（运行时生效）")
    public ResponseData<Boolean> updateWhitelistConfig(@RequestBody WhitelistConfigDto dto) {
        try {
            // 验证配置
            if (dto.getPaths() == null || dto.getPaths().isEmpty()) {
                return ResponseData.fail(400, "白名单路径不能为空");
            }
            
            // 验证路径格式
            for (String path : dto.getPaths()) {
                if (path == null || path.trim().isEmpty()) {
                    return ResponseData.fail(400, "白名单路径不能包含空值");
                }
            }
            
            // 更新配置（运行时生效）
            whitelistConfig.setPaths(dto.getPaths());
            whitelistConfig.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
            
            // 动态更新环境变量（可选，用于持久化到配置文件）
            updateEnvironmentProperty("auth.whitelist.paths", dto.getPaths());
            updateEnvironmentProperty("auth.whitelist.enabled", dto.getEnabled());
            
            log.info("白名单配置更新成功: paths={}, enabled={}", dto.getPaths(), dto.getEnabled());
            return ResponseData.ok(true);
        } catch (Exception e) {
            log.error("更新白名单配置失败", e);
            return ResponseData.fail(500, "更新白名单配置失败: " + e.getMessage());
        }
    }
    */

    /*
    @PostMapping("/whitelist/validate")
    @Operation(summary = "验证白名单路径", description = "验证指定路径是否匹配白名单规则")
    public ResponseData<Map<String, Object>> validateWhitelistPath(@RequestParam String path) {
        try {
            boolean isWhitelisted = isPathWhitelisted(path);
            
            Map<String, Object> result = new HashMap<>();
            result.put("path", path);
            result.put("isWhitelisted", isWhitelisted);
            result.put("matchedPattern", getMatchedPattern(path));
            
            return ResponseData.ok(result);
        } catch (Exception e) {
            log.error("验证白名单路径失败", e);
            return ResponseData.fail(500, "验证白名单路径失败: " + e.getMessage());
        }
    }
    */

    @GetMapping("/system-info")
    @Operation(summary = "获取系统信息", description = "获取系统运行时配置信息")
    public ResponseData<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> systemInfo = new HashMap<>();
            
            // 基本信息
            systemInfo.put("profiles", environment.getActiveProfiles());
            systemInfo.put("javaVersion", System.getProperty("java.version"));
            systemInfo.put("springBootVersion", getClass().getPackage().getImplementationVersion());
            
            // 认证相关配置
            Map<String, Object> authConfig = new HashMap<>();
            authConfig.put("whitelistEnabled", true); // whitelistConfig.getEnabled()
            authConfig.put("whitelistPathCount", 0); // whitelistConfig.getPaths() != null ? whitelistConfig.getPaths().size() : 0
            systemInfo.put("authConfig", authConfig);
            
            return ResponseData.ok(systemInfo);
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
            return ResponseData.fail(500, "获取系统信息失败: " + e.getMessage());
        }
    }

    /*
    // 检查路径是否在白名单中
    private boolean isPathWhitelisted(String path) {
        if (whitelistConfig.getPaths() == null || !whitelistConfig.getEnabled()) {
            return false;
        }
        
        org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();
        for (String pattern : whitelistConfig.getPaths()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        
        return false;
    }

    // 获取匹配的模式
    private String getMatchedPattern(String path) {
        if (whitelistConfig.getPaths() == null || !whitelistConfig.getEnabled()) {
            return null;
        }
        
        org.springframework.util.AntPathMatcher pathMatcher = new org.springframework.util.AntPathMatcher();
        for (String pattern : whitelistConfig.getPaths()) {
            if (pathMatcher.match(pattern, path)) {
                return pattern;
            }
        }
        
        return null;
    }
    */

    /**
     * 动态更新环境属性
     */
    private void updateEnvironmentProperty(String key, Object value) {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put(key, value);
            
            MapPropertySource propertySource = new MapPropertySource("dynamic-config", properties);
            environment.getPropertySources().addFirst(propertySource);
        } catch (Exception e) {
            log.warn("动态更新环境属性失败: key={}, value={}", key, value, e);
        }
    }
}