package cn.beinet.deployment.admin.auth;

import cn.beinet.business.login.service.OAuthAccountService;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/** 校验由后端直接调用供应商 HTTPS 用户接口取得的身份，随后创建或更新本站用户。 */
@Component
public class OAuthUsers {
    private final AuthProperties properties;
    private final OAuthAccountService accounts;
    public OAuthUsers(AuthProperties properties, OAuthAccountService accounts) {
        this.properties = properties;
        this.accounts = accounts;
    }
    public LoginUser google(Map<String, Object> profile) {
        String subject = value(profile.get("id"));
        String email = value(profile.get("email"));
        String context = context("google", subject, email, Boolean.TRUE.equals(profile.get("verified_email")));
        if (!Boolean.TRUE.equals(profile.get("verified_email"))) throw new LoginFailure("email_not_verified", context, null);
        if (!properties.allows(email)) throw new LoginFailure("email_not_allowed", context, null);
        return provision("google", subject, email, value(profile.get("name")));
    }
    public LoginUser github(Map<String, Object> profile, List<Map<String, Object>> emails) {
        String subject = value(profile.get("id"));
        String email = null;
        if (emails != null) {
            email = emails.stream().filter(item -> Boolean.TRUE.equals(item.get("verified")))
                    .filter(item -> item.get("email") instanceof String address && properties.allows(address))
                    .sorted((a,b) -> Boolean.compare(Boolean.TRUE.equals(b.get("primary")), Boolean.TRUE.equals(a.get("primary"))))
                    .map(item -> (String)item.get("email")).findFirst().orElse(null);
        }
        if (email == null) {
            String candidates = emails == null ? "unavailable" : emails.stream().limit(100)
                    .map(item -> "email=" + LoginDiagnostics.safe(item.get("email"))
                            + ",verified=" + Boolean.TRUE.equals(item.get("verified"))
                            + ",primary=" + Boolean.TRUE.equals(item.get("primary"))
                            + ",allowed=" + properties.allows(value(item.get("email"))))
                    .collect(java.util.stream.Collectors.joining(";"));
            throw new LoginFailure("verified_email_not_allowed", "provider=github subject="
                    + LoginDiagnostics.safe(subject) + " candidates=[" + candidates + "]", null);
        }
        return provision("github", subject, email, value(profile.get("name")));
    }
    private LoginUser provision(String provider, String subject, String email, String name) {
        if (subject == null || subject.isBlank()) throw new LoginFailure("login_denied");
        email = AuthProperties.normalize(email);
        try {
            long id = accounts.login(provider, subject, email, name);
            return new LoginUser(id, provider, subject, email, name == null ? email : name);
        } catch (RuntimeException e) {
            throw new LoginFailure(e instanceof IllegalStateException && "Account is disabled".equals(e.getMessage())
                    ? "account_disabled" : "account_storage_failed", context(provider, subject, email, true), e);
        }
    }
    private String context(String provider, String subject, String email, boolean verified) {
        return "provider=" + provider + " subject=" + LoginDiagnostics.safe(subject) + " email=" + LoginDiagnostics.safe(email)
                + " emailVerified=" + verified + " allowed=" + properties.allows(email) + " admin=" + properties.isAdmin(email);
    }
    private static String value(Object value) {
        return value instanceof String || value instanceof Number ? value.toString() : null;
    }
}
