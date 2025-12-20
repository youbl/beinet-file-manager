package cn.beinet.business.login.loginValidate;

import cn.beinet.business.login.service.TokenBlacklistService;
import cn.beinet.core.base.consts.ContextConst;
import cn.beinet.core.base.exceptions.BaseException;
import cn.beinet.core.utils.TokenHelper;
import cn.beinet.core.web.context.ContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 判断Header里是否存在token，并判断token是否有效登录信息的类
 *
 * @author youbl
 * @since 2023/1/4 18:18
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TokenValidator implements Validator {
    private final TokenBlacklistService tokenBlacklistService;
    
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
        String tokenName = HttpHeaders.AUTHORIZATION;
        if (!StringUtils.hasLength(tokenName)) {
            throw new BaseException(500, "未配置有效的tokenName");
        }

        // 优先从Authorization头获取Token
        String token = ContextUtils.getHeader(tokenName);
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            // 从Cookie获取Token
            token = ContextUtils.getCookie(ContextConst.LOGIN_COOKIE_NAME);
        } else {
            // 移除Bearer前缀
            token = token.substring(7);
        }
        
        log.debug("url:{} token: {}", request.getRequestURI(), token);
        
        // 检查Token是否为空
        if (!StringUtils.hasText(token)) {
            log.debug("Token为空: url={}", request.getRequestURI());
            return Result.fail();
        }
        
        // 检查Token是否在黑名单中
        if (tokenBlacklistService.isBlacklisted(token)) {
            log.debug("Token在黑名单中: url={}", request.getRequestURI());
            return Result.fail();
        }
        
        // 验证授权有效性, 并获取账号
        String account = TokenHelper.getAccountFromJwt(token, secret);
        // 验证token
        if (!StringUtils.hasLength(account)) {
            log.debug("Token无效: url={}", request.getRequestURI());
            return Result.fail();
        }
        
        log.debug("Token验证成功: url={}, account={}", request.getRequestURI(), account);
        return Result.ok(account);
    }
}
