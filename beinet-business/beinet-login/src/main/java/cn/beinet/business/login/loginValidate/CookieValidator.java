package cn.beinet.business.login.loginValidate;

import cn.beinet.core.base.consts.ContextConst;
import cn.beinet.core.base.exceptions.BaseException;
import cn.beinet.core.utils.TokenHelper;
import cn.beinet.core.web.context.ContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 判断Cookie是否存在有效登录信息的类
 *
 * @author youbl
 * @since 2023/1/4 18:18
 */
@Component
@Slf4j
public class CookieValidator implements Validator {
    @Value("${login.secret:beinet.cn.file}")
    private String secret;

    /**
     * 正常优先级
     *
     * @return 排序
     */
    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Result validated(HttpServletRequest request, HttpServletResponse response) {
        String cookName = ContextConst.LOGIN_COOKIE_NAME;
        if (!StringUtils.hasLength(cookName)) {
            throw new BaseException(500, "未配置有效的cookie名");
        }

        // 判断有没有cookie，有cookie时是否有效
        String token = ContextUtils.getCookie(cookName);

        log.debug("url:{} token: {}", request.getRequestURI(), token);
        // 验证授权有效性, 并获取账号
        String account = TokenHelper.getAccountFromJwt(token, secret);
        // 验证token
        if (!StringUtils.hasLength(account)) {
            return Result.fail();
        }
        return Result.ok(account);
    }
}
