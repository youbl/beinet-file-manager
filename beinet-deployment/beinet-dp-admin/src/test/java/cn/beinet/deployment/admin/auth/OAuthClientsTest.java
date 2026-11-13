package cn.beinet.deployment.admin.auth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class OAuthClientsTest {
    @ParameterizedTest
    @ValueSource(strings={"http://public.example", "https://user@files.example", "https://files.example/path", "https://files.example?q=1", "https://files.example#fragment"})
    void rejectsInvalidOrigin(String base) {
        var p = new AuthProperties(); p.setBaseUrl(base);
        assertThrows(IllegalStateException.class, () -> new OAuthClients(p));
    }
    @Test void noCredentialsDisablesProvidersAndPartialCredentialsFail() {
        var p = new AuthProperties(); assertTrue(new OAuthClients(p).names().isEmpty());
        p.getGoogle().setClientId("id"); assertThrows(IllegalStateException.class, () -> new OAuthClients(p));
    }
    @Test void callbackAndEndpointsComeFromConfiguration() {
        var p = new AuthProperties(); p.setBaseUrl("http://localhost:8999/");
        p.getGoogle().setClientId("id"); p.getGoogle().setClientSecret("secret");
        var c = new OAuthClients(p).require("google");
        assertEquals("http://localhost:8999/login/oauth2/code/google", c.redirectUri());
        assertTrue(c.scopes().contains("userinfo.email")); assertFalse(c.toString().contains("secret"));
    }
}
