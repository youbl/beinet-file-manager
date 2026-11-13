package cn.beinet.deployment.admin.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.time.Clock;

/** 每个 Session 保留一笔授权请求，五分钟过期；匹配后在会话锁内一次消费。 */
@Component
public class SessionAuthorizationRequests {
    private static final String KEY = SessionAuthorizationRequests.class.getName();
    private final Clock clock;
    public SessionAuthorizationRequests() { this(Clock.systemUTC()); }
    SessionAuthorizationRequests(Clock clock) { this.clock = clock; }

    public Pending start(HttpServletRequest request, String provider, String returnTo) {
        var pending = new Pending(provider, AuthSupport.randomToken(), AuthSupport.randomToken(),
                AuthSupport.safeReturnTo(returnTo), clock.millis() + 300_000);
        var session = request.getSession();
        synchronized (session) { session.setAttribute(KEY, pending); }
        return pending;
    }

    public Pending consume(HttpServletRequest request, String provider, String state) {
        var session = request.getSession(false);
        if (session == null) throw new LoginFailure("authorization_request_not_found");
        synchronized (session) {
            Object stored = session.getAttribute(KEY);
            if (!(stored instanceof Pending pending)) throw new LoginFailure("authorization_request_not_found");
            if (clock.millis() >= pending.expiresAt()) {
                session.removeAttribute(KEY);
                throw new LoginFailure("authorization_request_not_found");
            }
            if (!pending.provider().equals(provider) || !AuthSupport.same(pending.state(), state)) {
                throw new LoginFailure("invalid_state_parameter");
            }
            session.removeAttribute(KEY);
            return pending;
        }
    }

    // verifier 是 PKCE 原始随机值，仅存在服务端；returnTo 是经过校验的本站页面地址。
    public record Pending(String provider, String state, String verifier, String returnTo, long expiresAt)
            implements Serializable {
        @Override public String toString() { return "Pending[" + provider + "]"; }
    }
}
