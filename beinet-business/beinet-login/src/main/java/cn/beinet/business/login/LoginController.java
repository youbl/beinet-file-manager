package cn.beinet.business.login;

import cn.beinet.business.login.dal.UsersMapper;
import cn.beinet.business.login.dal.entity.Users;
import cn.beinet.business.login.service.AuditLogService;
import cn.beinet.business.login.service.LoginService;
import cn.beinet.business.login.service.TokenBlacklistService;
import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.core.base.consts.ContextConst;
import cn.beinet.core.utils.TokenHelper;
import cn.beinet.core.web.context.ContextUtils;
import cn.beinet.sdk.login.LoginSdk;
import cn.beinet.sdk.login.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "登录认证", description = "用户登录认证相关接口")
public class LoginController implements LoginSdk {
    private final LoginService loginService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditLogService auditLogService;
    private final UsersMapper usersMapper;

    @Value("${login.secret:beinet.cn.file}")
    private String secret;

    /**
     * 根据github授权码，去github获取用户信息，并完成登录
     *
     * @param code github的授权码
     * @return 用户信息，以及cookie里有token
     */
    @Override
    @Operation(summary = "GitHub OAuth 登录", description = "通过 GitHub 授权码完成用户登录")
    public ResponseData<UserDto> github(@RequestParam String code) {
        try {
            var ret = loginService.loginByGithub(code);
            log.info("GitHub登录成功: userId={}, email={}", ret.getId(), ret.getEmail());
            return ResponseData.ok(ret);
        } catch (Exception e) {
            log.error("GitHub登录失败: code={}", code, e);
            return ResponseData.fail(1001, "GitHub登录失败: " + e.getMessage());
        }
    }

    /**
     * 根据google的token，去google获取用户信息，并完成登录
     *
     * @param accessToken google的token
     * @return 用户信息，以及cookie里有token
     */
    @Override
    @Operation(summary = "Google OAuth 登录", description = "通过 Google Access Token 完成用户登录")
    public ResponseData<UserDto> google(@RequestParam String accessToken) {
        try {
            var ret = loginService.loginByGoogle(accessToken);
            log.info("Google登录成功: email={}", ret.getEmail());
            return ResponseData.ok(ret);
        } catch (Exception e) {
            log.error("Google登录失败: accessToken={}", accessToken, e);
            return ResponseData.fail(1002, "Google登录失败: " + e.getMessage());
        }
    }

    /**
     * 用户登出
     *
     * @return 登出结果
     */
    @Override
    @PostMapping("/login/logout") 
    @Operation(summary = "用户登出", description = "用户主动登出，使Token失效")
    public ResponseData<Void> logout() {
        HttpServletRequest request = getCurrentRequest();
        try {
            String token = getTokenFromRequest(request);
            if (!StringUtils.hasText(token)) {
                return ResponseData.fail(1003, "Token不存在");
            }

            // 获取用户信息
            String account = TokenHelper.getAccountFromJwt(token, secret);
            if (!StringUtils.hasText(account)) {
                return ResponseData.fail(1004, "Token无效");
            }

            // 查找用户
            Users user = usersMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Users>()
                    .eq("userEmail", account)
                    .eq("delflag", 0)
            );

            // 将Token加入黑名单
            tokenBlacklistService.addToBlacklist(token);

            // 清除Cookie
            ContextUtils.addCookie(ContextConst.LOGIN_COOKIE_NAME, "", 0);

            // 记录登出事件
            if (user != null) {
                auditLogService.recordLogout(user.getId(), request);
            }

            log.info("用户登出成功: account={}", account);
            return ResponseData.ok();
        } catch (Exception e) {
            log.error("用户登出失败", e);
            return ResponseData.fail(500, "登出失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息
     */
    @Override
    @GetMapping("/login/user/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public ResponseData<UserDto> getUserInfo() {
        HttpServletRequest request = getCurrentRequest();
        try {
            String token = getTokenFromRequest(request);
            if (!StringUtils.hasText(token)) {
                return ResponseData.fail(1003, "Token不存在");
            }

            // 检查Token是否在黑名单中
            if (tokenBlacklistService.isBlacklisted(token)) {
                return ResponseData.fail(1008, "Token已失效，请重新登录");
            }

            // 验证Token并获取用户信息
            String account = TokenHelper.getAccountFromJwt(token, secret);
            if (!StringUtils.hasText(account)) {
                return ResponseData.fail(1004, "Token无效或已过期");
            }

            // 查找用户
            Users user = usersMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Users>()
                    .eq("userEmail", account)
                    .eq("delflag", 0)
            );

            if (user == null) {
                return ResponseData.fail(2004, "用户不存在");
            }

            if (user.getStatus() == 0) {
                return ResponseData.fail(1005, "用户已被禁用");
            }

            // 构建用户信息
            UserDto userDto = buildUserDto(user);
            return ResponseData.ok(userDto);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResponseData.fail(500, "获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前HTTP请求
     * 
     * @return HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new RuntimeException("无法获取当前HTTP请求");
        }
        return attributes.getRequest();
    }

    /**
     * 从请求中获取Token
     *
     * @param request HTTP请求
     * @return Token字符串
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 优先从Authorization头获取
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 从Cookie获取
        return ContextUtils.getCookie(ContextConst.LOGIN_COOKIE_NAME);
    }

    /**
     * 构建用户DTO
     *
     * @param user 用户实体
     * @return 用户DTO
     */
    private UserDto buildUserDto(Users user) {
        return new UserDto()
                .setId(user.getId())
                .setUsername(user.getName())
                .setEmail(user.getUserEmail())
                .setAvatar(user.getPicture())
                .setStatus(user.getStatus())
                .setLastLoginDate(user.getLastLoginDate())
                .setLoginCount(user.getLoginCount())
                .setLoginType(getLoginTypeDescription(user.getLoginType()))
                .setGithubId(user.getGithubId())
                .setGithubLogin(user.getGithubLogin())
                .setGithubAvatarUrl(user.getGithubAvatarUrl())
                .setGithubNodeId(user.getGithubNodeId())
                .setLastLoginIp(user.getLastLoginIp())
                .setCreateTime(user.getCreateTime())
                .setUpdateTime(user.getUpdateTime());
    }

    /**
     * 获取登录类型描述
     *
     * @param loginType 登录类型
     * @return 描述
     */
    private String getLoginTypeDescription(Integer loginType) {
        if (loginType == null) {
            return "未知";
        }
        return switch (loginType) {
            case 1 -> "GitHub";
            case 2 -> "Google";
            case 3 -> "密码";
            default -> "未知";
        };
    }
}
