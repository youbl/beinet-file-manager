package cn.beinet.business.login.loginValidate;

import cn.beinet.core.base.consts.ContextConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

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
public class NoNeedLoginValidator implements Validator {
    // 无须登录认证的url正则
    private static final Pattern patternRequest = Pattern.compile("(?i)" +
            "^/login" +
            "|^/2fa\\.html" +
            "|^/actuator/health" +      // 健康检查接口
            "|^/swagger" +
            "|/api-docs/" + // http://127.0.0.1:8999/ab-ops/v3/api-docs/swagger-config
            "|^/doc\\.html" +
            "|^/test" +
            "|\\.(gif|ico|jpg|png|bmp|txt|xml|js|css|ttf|woff|map)$");// |html?

    private final Environment environment;

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
        Matcher matcher = patternRequest.matcher(url);
        if (matcher.find() || customUrl(url))
            return Result.ok(ContextConst.ANONYMOUS);
        return Result.fail();
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
