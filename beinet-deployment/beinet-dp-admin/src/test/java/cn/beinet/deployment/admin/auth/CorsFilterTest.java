package cn.beinet.deployment.admin.auth;

import cn.beinet.business.login.service.OAuthAccountService;
import cn.beinet.deployment.admin.autoConfig.AuthorizationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsFilterTest {
    private final AuthProperties properties = new AuthProperties();
    private final OAuthAccountService accounts = mock(OAuthAccountService.class);

    private MockHttpServletResponse run(String method, String origin, String requestedMethod,
                                        String headers, MockHttpSession session, String csrf) throws Exception {
        properties.getCors().setAllowedOrigins(List.of("https://app.example.com"));
        var request = new MockHttpServletRequest(method, "/stores/list");
        if (origin != null) request.addHeader("Origin", origin);
        if (requestedMethod != null) request.addHeader("Access-Control-Request-Method", requestedMethod);
        if (headers != null) request.addHeader("Access-Control-Request-Headers", headers);
        if (session != null) request.setSession(session);
        if (csrf != null) request.addHeader("X-XSRF-TOKEN", csrf);
        var response = new MockHttpServletResponse();
        new AuthorizationFilter(properties, accounts).doFilter(request, response,
                (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setHeader("Business-Reached", "yes"));
        return response;
    }

    @Test void preflightDoesNotNeedSessionOrDatabase() throws Exception {
        var response = run("OPTIONS", "https://app.example.com", "POST", "Content-Type, X-XSRF-TOKEN", null, null);
        assertEquals(200, response.getStatus());
        assertEquals("https://app.example.com", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        assertNull(response.getHeader("Business-Reached"));
        assertNull(response.getHeader("Set-Cookie"));
        verifyNoInteractions(accounts);
    }

    @Test void refusesUntrustedOriginsMethodsAndHeaders() throws Exception {
        assertEquals(403, run("OPTIONS", "https://evil.example", "GET", null, null, null).getStatus());
        assertEquals(403, run("OPTIONS", "null", "GET", null, null, null).getStatus());
        assertEquals(403, run("OPTIONS", "https://app.example.com", "TRACE", null, null, null).getStatus());
        assertEquals(403, run("OPTIONS", "https://app.example.com", "POST", "X-User-Id", null, null).getStatus());
        verifyNoInteractions(accounts);
    }

    @Test void optionsAndActualRequestsDoNotBypassLogin() throws Exception {
        assertEquals(401, run("OPTIONS", null, null, null, null, null).getStatus());
        assertEquals(401, run("OPTIONS", "https://app.example.com", null, null, null, null).getStatus());
        var response = run("GET", "https://app.example.com", null, null, null, null);
        assertEquals(401, response.getStatus());
        assertEquals("https://app.example.com", response.getHeader("Access-Control-Allow-Origin"));
        verifyNoInteractions(accounts);
    }

    @Test void authenticatedWritesStillRequireSessionCsrf() throws Exception {
        properties.setAllowedEmails(List.of("owner@example.com"));
        when(accounts.isEnabled(1L)).thenReturn(true);
        var session = new MockHttpSession();
        session.setAttribute(LoginUser.SESSION_KEY, new LoginUser(1, "github", "123", "owner@example.com", "Owner"));
        var read = run("GET", "https://app.example.com", null, null, session, null);
        String token = read.getHeader("X-XSRF-TOKEN");
        assertNotNull(token);
        assertEquals(403, run("POST", "https://app.example.com", null, null, session, null).getStatus());
        var write = run("POST", "https://app.example.com", null, null, session, token);
        assertEquals(200, write.getStatus());
        assertEquals("yes", write.getHeader("Business-Reached"));
        assertEquals(403, run("POST", "https://evil.example", null, null, session, token).getStatus());
    }

    @Test void originMatchingIsExactAndDefaultsToSameOriginOnly() {
        assertTrue(properties.allowsOrigin("http://localhost:8080"));
        assertFalse(properties.allowsOrigin("https://app.example.com"));
        properties.getCors().setAllowedOrigins(List.of("*", "null", "https://app.example.com/path"));
        assertFalse(properties.allowsOrigin("*"));
        assertFalse(properties.allowsOrigin("null"));
        assertFalse(properties.allowsOrigin("https://app.example.com/path"));
    }
}
