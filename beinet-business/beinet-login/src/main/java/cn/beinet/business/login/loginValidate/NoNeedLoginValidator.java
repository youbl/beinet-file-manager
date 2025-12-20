package cn.beinet.business.login.loginValidate;

import cn.beinet.business.login.config.WhitelistConfig;
import cn.beinet.core.base.consts.ContextConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 那些不需要登录的url验证
 *
 * @author youbl
 * @since 2023/1/4 18:18
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NoNeedLoginValidator implements Validator {
    private final Environment environment;
    private final WhitelistConfig whitelistConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 无须登录认证的url正则（保留原有的硬编码规则作为基础）
    private static final Pattern patternRequest = Pattern.compile("(?i)" +
            "^/login" +
            "|^/2fa\\.html" +
            "|^/actuator/health" +      // 健康检查接口
            "|^/swagger" +
            "|/api-docs/" + // http://127.0.0.1:8999/ab-ops/v3/api-docs/swagger-config
            "|^/doc\\.html" +
            "|^/test" +
            "|\\.(gif|ico|jpg|png|bmp|txt|xml|js|css|ttf|woff|map)$");// |html?

    /**
     * 优先级最高
     *
     * @return 排序
     */
    @Override
    public int getOrder() {
        return -999;
    }

    @Override
    public Result validated(HttpServletRequest request, HttpServletResponse response) {
        //request.getRequestURL() 带有域名，所以不用
        //request.getRequestURI() 带有ContextPath，所以不用
        String url = request.getServletPath();
        
        // 检查硬编码的白名单规则
        Matcher matcher = patternRequest.matcher(url);
        if (matcher.find()) {
            log.debug("URL匹配硬编码白名单: {}", url);
            return Result.ok(ContextConst.ANONYMOUS);
        }
        
        // 检查自定义URL规则
        if (customUrl(url)) {
            log.debug("URL匹配自定义白名单: {}", url);
            return Result.ok(ContextConst.ANONYMOUS);
        }
        
        // 检查配置文件中的白名单路径
        if (isWhitelistedPath(url)) {
            log.debug("URL匹配配置白名单: {}", url);
            return Result.ok(ContextConst.ANONYMOUS);
        }
        
        log.debug("URL需要认证: {}", url);
        return Result.fail();
    }

    /**
     * 检查URL是否在配置的白名单中
     * 
     * @param url 请求URL
     * @return 是否在白名单中
     */
    private boolean isWhitelistedPath(String url) {
        if (whitelistConfig.getPaths() == null || whitelistConfig.getPaths().isEmpty()) {
            return false;
        }
        
        for (String pattern : whitelistConfig.getPaths()) {
            if (pathMatcher.match(pattern, url)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean customUrl(String url) {
        // 验证码查看页面，生产环境不开放，其它环境都开放匿名权限
        var env = environment.getProperty("spring.profiles.active");
        if (env.equals("prod"))
            return false;

        if (url.equals("/userLog/msg.html") ||
                url.equals("/userMsgLogsAdmin/page") ||
                url.equals("/v3/api-docs"))
            return true;
        return false;
    }
}
