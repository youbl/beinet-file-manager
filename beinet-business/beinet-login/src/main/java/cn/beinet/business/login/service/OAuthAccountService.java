package cn.beinet.business.login.service;

import cn.beinet.business.login.dal.OAuthIdentityMapper;
import cn.beinet.business.login.dal.UsersMapper;
import cn.beinet.business.login.dal.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 以供应商和 subject 唯一识别账号；相同邮箱不会自动合并第三方身份。 */
@Service
@RequiredArgsConstructor
public class OAuthAccountService {
    private final OAuthIdentityMapper identities;
    private final UsersMapper users;

    // 用户与身份关联在同一事务中创建，任意一步失败均回滚，避免遗留孤立用户。
    @Transactional
    public Long login(String provider, String subject, String email, String name) {
        Long id = identities.findUserId(provider, subject);
        LocalDateTime now = LocalDateTime.now();
        // 已有身份必须复核状态，不能通过重新授权绕过禁用或软删除。
        if (id != null) {
            if (!isEnabled(id)) {
                throw new IllegalStateException("Account is disabled");
            }
            Users update = new Users().setId(id).setUserEmail(email);
            users.updateById(update);
            users.updateLastLoginInfo(id, "", now);
            return id;
        }
        Users user = new Users()
                .setName(name == null || name.isBlank() ? email : name.substring(0, Math.min(255, name.length())))
                .setUserEmail(email).setStatus(1).setDelflag(0).setIsAdmin(0).setIsBoss(0)
                .setLoginType("github".equals(provider) ? 1 : 2)
                .setLastLoginDate(now).setLoginCount(1);
        users.insert(user);
        // provider/subject 唯一约束防止并发首次登录重复关联。
        // 插入冲突时整个事务回滚，包括本次新增的 users 记录。
        identities.insert(provider, subject, user.getId());
        return user.getId();
    }

    /** 登录和后续业务请求共用此规则：用户存在、启用且未软删除。 */
    public boolean isEnabled(Long id) {
        Users user = users.selectById(id);
        return user != null && Integer.valueOf(1).equals(user.getStatus())
                && Integer.valueOf(0).equals(user.getDelflag());
    }
}
