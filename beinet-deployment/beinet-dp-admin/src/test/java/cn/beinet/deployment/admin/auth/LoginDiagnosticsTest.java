package cn.beinet.deployment.admin.auth;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;
class LoginDiagnosticsTest {
    @Test void databaseCauseExcludesSensitiveMessages() {
        var e=new LoginFailure("account_storage_failed","provider=google",new SQLException("password=secret","42S02",1146));
        assertEquals("account_storage_failed",LoginDiagnostics.reason(e));
        assertTrue(LoginDiagnostics.details(e).contains("SQLState=42S02 vendorCode=1146"));
        assertFalse(LoginDiagnostics.details(e).contains("secret"));
    }
    @Test void untrustedReasonAndControlCharactersAreFiltered() {
        assertEquals("unexpected_login_error",LoginDiagnostics.reason(new LoginFailure("secret&injected=true")));
        assertEquals("a b c",LoginDiagnostics.safe("a\nb\rc"));
    }
}
