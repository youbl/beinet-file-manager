package cn.beinet.business.login.service;

import cn.beinet.business.login.dal.entity.AuditLogs;
import cn.beinet.business.login.dto.AuditLogDto;
import cn.beinet.core.base.consts.ContextConst;
import cn.beinet.core.thirdparty.github.GithubUtil;
import cn.beinet.core.thirdparty.github.feigns.dto.GithubUserDto;
import cn.beinet.core.thirdparty.google.GoogleLoginUtil;
import cn.beinet.core.thirdparty.google.dto.GoogleInfoResult;
import cn.beinet.core.utils.TokenHelper;
import cn.beinet.core.web.context.ContextUtils;
import cn.beinet.deployment.admin.users.dal.UsersMapper;
import cn.beinet.deployment.admin.users.dal.entity.Users;
import cn.beinet.sdk.login.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginService {
    private final GithubUtil githubUtil;
    private final GoogleLoginUtil googleLoginUtil;
    private final UsersMapper usersMapper;
    private final AuditLogService auditLogService;

    @Value("${login.secret:beinet.cn.file}")
    private String secret;

    /**
     * 根据github授权码，去github获取用户信息，并完成登录
     *
     * @param code github的授权码
     * @return 用户信息，以及token
     */
    public UserDto loginByGithub(String code) {
        HttpServletRequest request = getCurrentRequest();
        
        try {
            // 记录GitHub认证开始
            auditLogService.recordGithubAuthStart(request);
            
            // 获取GitHub用户信息
            var githubUser = githubUtil.getUser(code);
            var ret = fromGitUser(githubUser);
            
            // 查找或创建用户
            Users user = findOrCreateGithubUser(githubUser, request);
            
            // 更新用户信息并生成Token
            updateUserInfoAndGenerateToken(user, ret, request);
            
            // 记录登录成功和GitHub认证成功
            auditLogService.recordLoginSuccess(user.getId(), request);
            auditLogService.recordGithubAuthSuccess(user.getId(), request);
            
            log.info("GitHub登录成功: userId={}, githubId={}, email={}", 
                    user.getId(), githubUser.getId(), githubUser.getEmail());
            
            return ret;
        } catch (Exception e) {
            // 记录GitHub认证失败
            auditLogService.recordGithubAuthFailed(request, e.getMessage());
            auditLogService.recordLoginFailed(request, "GitHub OAuth认证失败: " + e.getMessage());
            
            log.error("GitHub登录失败: code={}", code, e);
            throw new RuntimeException("GitHub登录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据google的token，去google获取用户信息，并完成登录
     *
     * @param accessToken google的token
     * @return 用户信息，以及token
     */
    public UserDto loginByGoogle(String accessToken) {
        HttpServletRequest request = getCurrentRequest();
        
        try {
            var user = googleLoginUtil.getUserInfoByToken(accessToken);
            var ret = fromGoogleUser(user);
            generateAndSetTokenToCookie(ret);
            
            // 记录登录成功
            // TODO: 实现Google用户的数据库存储逻辑
            auditLogService.recordLoginSuccess(null, request);
            
            return ret;
        } catch (Exception e) {
            auditLogService.recordLoginFailed(request, "Google OAuth认证失败: " + e.getMessage());
            log.error("Google登录失败: accessToken={}", accessToken, e);
            throw new RuntimeException("Google登录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找或创建GitHub用户
     * 
     * @param githubUser GitHub用户信息
     * @param request HTTP请求
     * @return 用户实体
     */
    private Users findOrCreateGithubUser(GithubUserDto githubUser, HttpServletRequest request) {
        // 先根据GitHub ID查找用户
        Users existingUser = usersMapper.selectByGithubId(githubUser.getId());
        
        if (existingUser != null) {
            // 更新GitHub信息（可能用户修改了GitHub用户名或头像）
            updateGithubUserInfo(existingUser, githubUser);
            return existingUser;
        }
        
        // 根据邮箱查找是否已有用户
        if (StringUtils.hasText(githubUser.getEmail())) {
            existingUser = usersMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Users>()
                    .eq("userEmail", githubUser.getEmail())
                    .eq("delflag", 0)
            );
            
            if (existingUser != null) {
                // 绑定GitHub信息到现有用户
                bindGithubToExistingUser(existingUser, githubUser);
                return existingUser;
            }
        }
        
        // 创建新用户
        return createNewGithubUser(githubUser, request);
    }

    /**
     * 更新现有用户的GitHub信息
     * 
     * @param user 现有用户
     * @param githubUser GitHub用户信息
     */
    private void updateGithubUserInfo(Users user, GithubUserDto githubUser) {
        boolean needUpdate = false;
        
        if (!githubUser.getLogin().equals(user.getGithubLogin())) {
            user.setGithubLogin(githubUser.getLogin());
            needUpdate = true;
        }
        
        if (!githubUser.getAvatar_url().equals(user.getGithubAvatarUrl())) {
            user.setGithubAvatarUrl(githubUser.getAvatar_url());
            user.setPicture(githubUser.getAvatar_url()); // 同步更新头像
            needUpdate = true;
        }
        
        if (StringUtils.hasText(githubUser.getName()) && 
            !githubUser.getName().equals(user.getName())) {
            user.setName(githubUser.getName());
            needUpdate = true;
        }
        
        if (needUpdate) {
            usersMapper.updateById(user);
            log.info("更新用户GitHub信息: userId={}, githubLogin={}", 
                    user.getId(), githubUser.getLogin());
        }
    }

    /**
     * 绑定GitHub信息到现有用户
     * 
     * @param user 现有用户
     * @param githubUser GitHub用户信息
     */
    private void bindGithubToExistingUser(Users user, GithubUserDto githubUser) {
        user.setGithubId(githubUser.getId())
            .setGithubLogin(githubUser.getLogin())
            .setGithubAvatarUrl(githubUser.getAvatar_url())
            .setGithubNodeId(githubUser.getNode_id())
            .setLoginType(1); // GitHub登录
        
        // 更新头像和用户名（如果GitHub有更好的信息）
        if (StringUtils.hasText(githubUser.getAvatar_url())) {
            user.setPicture(githubUser.getAvatar_url());
        }
        if (StringUtils.hasText(githubUser.getName())) {
            user.setName(githubUser.getName());
        }
        
        usersMapper.updateById(user);
        log.info("绑定GitHub信息到现有用户: userId={}, githubId={}", 
                user.getId(), githubUser.getId());
    }

    /**
     * 创建新的GitHub用户
     * 
     * @param githubUser GitHub用户信息
     * @param request HTTP请求
     * @return 新创建的用户
     */
    private Users createNewGithubUser(GithubUserDto githubUser, HttpServletRequest request) {
        Users newUser = new Users()
                .setName(StringUtils.hasText(githubUser.getName()) ? 
                        githubUser.getName() : githubUser.getLogin())
                .setUserEmail(githubUser.getEmail())
                .setPicture(githubUser.getAvatar_url())
                .setStatus(1) // 启用状态
                .setLoginType(1) // GitHub登录
                .setGithubId(githubUser.getId())
                .setGithubLogin(githubUser.getLogin())
                .setGithubAvatarUrl(githubUser.getAvatar_url())
                .setGithubNodeId(githubUser.getNode_id())
                .setLoginCount(0)
                .setDelflag(0);
        
        // 设置注册IP
        String clientIp = getClientIpAddress(request);
        if (StringUtils.hasText(clientIp)) {
            newUser.setUserIp(clientIp);
        }
        
        usersMapper.insert(newUser);
        log.info("创建新GitHub用户: userId={}, githubId={}, email={}", 
                newUser.getId(), githubUser.getId(), githubUser.getEmail());
        
        return newUser;
    }

    /**
     * 更新用户信息并生成Token
     * 
     * @param user 用户实体
     * @param userDto 用户DTO
     * @param request HTTP请求
     */
    private void updateUserInfoAndGenerateToken(Users user, UserDto userDto, HttpServletRequest request) {
        // 更新最后登录信息
        String clientIp = getClientIpAddress(request);
        usersMapper.updateLastLoginInfo(user.getId(), clientIp, LocalDateTime.now());
        
        // 设置用户信息到DTO
        userDto.setId(user.getId())
               .setStatus(user.getStatus())
               .setLastLoginDate(LocalDateTime.now())
               .setLoginCount(user.getLoginCount() + 1)
               .setLoginType("GitHub")
               .setLastLoginIp(clientIp);
        
        // 生成Token
        generateAndSetTokenToCookie(userDto);
    }

    private void generateAndSetTokenToCookie(UserDto user) {
        Assert.hasLength(user.getEmail(), "user email is empty.");
        if (!StringUtils.hasLength(user.getUsername())) {
            user.setUsername(user.getEmail());
        }

        long expireSeconds = ContextConst.LOGIN_EXPIRE_SECOND;
        // 完成登录，并生成token
        var jwt = TokenHelper.signJwt(user.getEmail(), secret, expireSeconds);
        user.setToken(jwt);

        ContextUtils.addCookie(ContextConst.LOGIN_COOKIE_NAME, jwt, expireSeconds);
    }

    private UserDto fromGitUser(GithubUserDto githubUser) {
        return new UserDto()
                .setUsername(githubUser.getName())
                .setEmail(githubUser.getEmail())
                .setAvatar(githubUser.getAvatar_url())
                .setGithubId(githubUser.getId())
                .setGithubLogin(githubUser.getLogin())
                .setGithubAvatarUrl(githubUser.getAvatar_url())
                .setGithubNodeId(githubUser.getNode_id());
    }

    private UserDto fromGoogleUser(GoogleInfoResult googleUser) {
        return new UserDto()
                .setUsername(googleUser.getName())
                .setEmail(googleUser.getEmail());
    }

    /**
     * 获取当前HTTP请求
     * 
     * @return HTTP请求对象
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
