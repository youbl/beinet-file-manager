package cn.beinet.deployment.admin.auth;

import java.io.Serializable;

/** HttpSession 中保存的本站登录身份；不包含供应商 Token，管理员权限每次请求重新计算。 */
public record LoginUser(long id, String provider, String subject, String email, String name) implements Serializable {
    // Session 属性的 key 名，对应的 value 为 LoginUser 对象。
    public static final String SESSION_KEY = LoginUser.class.getName();
}
