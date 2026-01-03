package cn.beinet.business.login.service;

import cn.beinet.business.login.dal.UsersMapper;
import cn.beinet.business.login.dal.entity.Users;
import cn.beinet.sdk.login.dto.UserDto;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserManagementService {

    private final UsersMapper usersMapper;
    private final AuditLogService auditLogService;

    /**
     * 分页查询用户列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词（用户名、邮箱）
     * @param status 用户状态（可选）
     * @param loginType 登录类型（可选）
     * @return 分页结果
     */
    public IPage<UserDto> queryUsers(int page, int size, String keyword, Integer status, Integer loginType) {
        Page<Users> pageParam = new Page<>(page, size);
        
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("delflag", 0);
        
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like("name", keyword)
                .or()
                .like("userEmail", keyword)
                .or()
                .like("githubLogin", keyword)
            );
        }
        
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        if (loginType != null) {
            queryWrapper.eq("loginType", loginType);
        }
        
        queryWrapper.orderByDesc("createTime");
        
        IPage<Users> userPage = usersMapper.selectPage(pageParam, queryWrapper);
        
        // 转换为DTO
        IPage<UserDto> result = new Page<>(page, size, userPage.getTotal());
        List<UserDto> userDtos = userPage.getRecords().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        result.setRecords(userDtos);
        
        return result;
    }

    /**
     * 根据ID获取用户详情
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserDto getUserById(Long userId) {
        Users user = usersMapper.selectById(userId);
        if (user == null || user.getDelflag() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        return convertToDto(user);
    }

    /**
     * 更新用户状态
     * 
     * @param userId 用户ID
     * @param status 新状态（1-启用，0-禁用）
     * @param request HTTP请求
     * @return 是否成功
     */
    public boolean updateUserStatus(Long userId, Integer status, HttpServletRequest request) {
        Users user = usersMapper.selectById(userId);
        if (user == null || user.getDelflag() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        if (user.getStatus().equals(status)) {
            return true; // 状态未变化
        }
        
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        
        int updated = usersMapper.updateById(user);
        
        if (updated > 0) {
            // 记录审计日志
            String action = status == 1 ? "启用" : "禁用";
            auditLogService.recordAuditLog(
                cn.beinet.business.login.dto.AuditLogDto.builder()
                    .userId(userId)
                    .eventType("USER_STATUS_CHANGE")
                    .eventDescription(String.format("管理员%s用户: %s", action, user.getName()))
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .requestUri(request.getRequestURI())
                    .success()
                    .build()
            );
            
            log.info("用户状态更新成功: userId={}, status={}", userId, status);
        }
        
        return updated > 0;
    }

    /**
     * 删除用户（软删除）
     * 
     * @param userId 用户ID
     * @param request HTTP请求
     * @return 是否成功
     */
    public boolean deleteUser(Long userId, HttpServletRequest request) {
        Users user = usersMapper.selectById(userId);
        if (user == null || user.getDelflag() == 1) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setDelflag(1);
        user.setUpdateTime(LocalDateTime.now());
        
        int updated = usersMapper.updateById(user);
        
        if (updated > 0) {
            // 记录审计日志
            auditLogService.recordAuditLog(
                cn.beinet.business.login.dto.AuditLogDto.builder()
                    .userId(userId)
                    .eventType("USER_DELETE")
                    .eventDescription(String.format("管理员删除用户: %s", user.getName()))
                    .ipAddress(getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .requestUri(request.getRequestURI())
                    .success()
                    .build()
            );
            
            log.info("用户删除成功: userId={}", userId);
        }
        
        return updated > 0;
    }

    /**
     * 获取用户统计信息
     * 
     * @return 统计信息
     */
    public UserStatistics getUserStatistics() {
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("delflag", 0);
        
        Long totalUsers = usersMapper.selectCount(queryWrapper);
        
        queryWrapper.eq("status", 1);
        Long activeUsers = usersMapper.selectCount(queryWrapper);
        
        queryWrapper.clear();
        queryWrapper.eq("delflag", 0).eq("status", 0);
        Long disabledUsers = usersMapper.selectCount(queryWrapper);
        
        queryWrapper.clear();
        queryWrapper.eq("delflag", 0).eq("loginType", 1);
        Long githubUsers = usersMapper.selectCount(queryWrapper);
        
        queryWrapper.clear();
        queryWrapper.eq("delflag", 0).eq("loginType", 2);
        Long googleUsers = usersMapper.selectCount(queryWrapper);
        
        return UserStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)
                .githubUsers(githubUsers)
                .googleUsers(googleUsers)
                .build();
    }

    /**
     * 转换实体为DTO
     * 
     * @param user 用户实体
     * @return 用户DTO
     */
    private UserDto convertToDto(Users user) {
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

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
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

    /**
     * 用户统计信息
     */
    @lombok.Builder
    @lombok.Data
    public static class UserStatistics {
        private Long totalUsers;
        private Long activeUsers;
        private Long disabledUsers;
        private Long githubUsers;
        private Long googleUsers;
    }
}