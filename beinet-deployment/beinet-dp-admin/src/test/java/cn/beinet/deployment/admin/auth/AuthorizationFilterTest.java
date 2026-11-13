package cn.beinet.deployment.admin.auth;

import cn.beinet.business.login.service.OAuthAccountService;
import cn.beinet.deployment.admin.autoConfig.AuthorizationFilter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthorizationFilterTest {
    AuthProperties p;
    OAuthAccountService accounts;
    OAuthGateway gateway;
    MockMvc mvc;
    @BeforeEach void setup() {
        p=new AuthProperties();p.setAllowedEmails(List.of("owner@example.com"));
        p.getGoogle().setClientId("google-id");p.getGoogle().setClientSecret("google-secret");
        p.getGithub().setClientId("github-id");p.getGithub().setClientSecret("github-secret");
        accounts=mock(OAuthAccountService.class);gateway=mock(OAuthGateway.class);
        when(accounts.isEnabled(1L)).thenReturn(true);when(accounts.login(any(),any(),any(),any())).thenReturn(1L);
        var controller=new AuthController(new OAuthClients(p),new SessionAuthorizationRequests(),gateway,new OAuthUsers(p,accounts),p);
        mvc=MockMvcBuilders.standaloneSetup(controller,new Pages()).addFilters(new AuthorizationFilter(p,accounts)).build();
    }
    MockHttpSession loggedIn() {
        var s=new MockHttpSession();s.setAttribute(LoginUser.SESSION_KEY,new LoginUser(1,"google","g1","owner@example.com","Owner"));return s;
    }
    String csrf(MockHttpSession session) throws Exception {
        return mvc.perform(get("/auth/session").session(session)).andReturn().getResponse().getCookie("XSRF-TOKEN").getValue();
    }
    @ParameterizedTest @ValueSource(strings={"/stores/list","/users","/actuator/health","/v3/api-docs","/error","/private.txt"})
    void anonymousBusinessRequestsAreRejected(String path) throws Exception {
        mvc.perform(get(path).header("x-user-id","1").header("Authorization","Bearer old")).andExpect(status().isUnauthorized());
    }
    @Test void pagesRedirectAndPublicAssetsDoNotQueryDatabase() throws Exception {
        mvc.perform(get("/stores/store.html")).andExpect(status().isFound());
        mvc.perform(get("/login.html")).andExpect(status().isOk());mvc.perform(get("/res/test.js")).andExpect(status().isOk());
        mvc.perform(get("/auth/providers")).andExpect(jsonPath("$.data[0]").value("google"));verifyNoInteractions(accounts);
        mvc.perform(post("/res/test.js")).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest @ValueSource(strings={"/users","/users/1","/api/aliyun","/aliyun","/aws","/tools","/config","/audit-logs"})
    void adminPathsRejectOrdinaryUsers(String path) throws Exception {
        mvc.perform(get(path).session(loggedIn())).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings={"/login/github","/login/google","/github/callback","/google/token","/demo","/demo/test.html","/tools/ymldecrypt","/stores/extractRar","/stores/singleDir"})
    void disabledPathsRejectAdmins(String path) throws Exception {
        p.setAdminEmails(List.of("owner@example.com"));mvc.perform(get(path).session(loggedIn())).andExpect(status().isForbidden());
    }
    @Test void adminOnlyEmailQualifiesAndSimilarPrefixIsNotAdmin() throws Exception {
        p.setAllowedEmails(List.of());p.setAdminEmails(List.of("owner@example.com"));
        mvc.perform(get("/users").session(loggedIn())).andExpect(status().isOk());
        p.setAllowedEmails(List.of("owner@example.com"));p.setAdminEmails(List.of());
        mvc.perform(get("/users-other").session(loggedIn())).andExpect(status().isNotFound());
    }
    @Test void removedEmailAndDisabledAccountInvalidateWhileDatabaseFailureReturns503() throws Exception {
        var session=loggedIn();p.setAllowedEmails(List.of());mvc.perform(get("/stores/list").session(session)).andExpect(status().isUnauthorized());assertTrue(session.isInvalid());
        p.setAllowedEmails(List.of("owner@example.com"));when(accounts.isEnabled(1L)).thenReturn(false);
        mvc.perform(get("/stores/list").session(loggedIn())).andExpect(status().isUnauthorized());
        when(accounts.isEnabled(1L)).thenThrow(new RuntimeException("db unavailable"));session=loggedIn();
        mvc.perform(get("/stores/list").session(session)).andExpect(status().isServiceUnavailable());assertFalse(session.isInvalid());
    }
    @Test void csrfIsSessionBoundAndOriginMustMatch() throws Exception {
        var s=loggedIn();String token=csrf(s);
        mvc.perform(post("/stores/uploadFile").session(s)).andExpect(status().isForbidden());
        mvc.perform(post("/stores/uploadFile").session(loggedIn()).header("X-XSRF-TOKEN",token)).andExpect(status().isForbidden());
        mvc.perform(post("/stores/uploadFile").session(s).header("X-XSRF-TOKEN",token).header("Origin","https://evil.test")).andExpect(status().isForbidden());
        mvc.perform(post("/stores/uploadFile").session(s).header("X-XSRF-TOKEN",token)).andExpect(status().isOk());
    }
    @Test void logoutInvalidatesSessionAndClearsCookies() throws Exception {
        var s=loggedIn();String token=csrf(s);
        mvc.perform(post("/auth/logout").session(s).header("X-XSRF-TOKEN",token)).andExpect(status().isOk()).andExpect(cookie().maxAge("JSESSIONID",0));
        assertTrue(s.isInvalid());
    }
    @ParameterizedTest @ValueSource(strings={"/users;anything","/users/../stores/list","/users//1","/users%2F1"})
    void ambiguousPathsAreRejected(String path) throws Exception {
        mvc.perform(get("/").with(request -> { request.setRequestURI(path); request.setServletPath(path); return request; }).session(loggedIn()))
                .andExpect(status().isBadRequest());
    }
    @ParameterizedTest @ValueSource(strings={"https://evil.test","//evil.test","///evil.html","/../evil.html","/login.html","/demo/test.html"})
    void invalidReturnTargetsUseDefault(String target) { assertEquals("/stores/store.html",AuthSupport.safeReturnTo(target)); }
    @ParameterizedTest @ValueSource(strings={"google","github"})
    void oauthEndToEndValidatesStateRotatesSessionAndUsesPkce(String provider) throws Exception {
        var start=mvc.perform(get("/oauth2/authorization/"+provider).param("returnTo","/users/users.html")).andExpect(status().isFound()).andReturn();
        String location=start.getResponse().getRedirectedUrl();assertTrue(location.contains("code_challenge_method=S256"));assertFalse(location.contains("secret"));
        var old=(MockHttpSession)start.getRequest().getSession();
        var pending=(SessionAuthorizationRequests.Pending)old.getAttribute(SessionAuthorizationRequests.class.getName());
        when(gateway.exchange(any(),eq("valid-code"),eq(pending.verifier()))).thenReturn("private-token");
        when(gateway.profile(any(),eq("private-token"))).thenReturn(Map.of("id","g1","email","owner@example.com","verified_email",true));
        when(gateway.githubEmails("private-token")).thenReturn(List.of(Map.of("email","owner@example.com","verified",true)));
        var result=mvc.perform(get("/login/oauth2/code/"+provider).session(old).param("state",pending.state()).param("code","valid-code"))
                .andExpect(redirectedUrl("/users/users.html")).andReturn();
        assertTrue(old.isInvalid());var fresh=(MockHttpSession)result.getRequest().getSession();assertNotEquals(old.getId(),fresh.getId());
        assertInstanceOf(LoginUser.class,fresh.getAttribute(LoginUser.SESSION_KEY));
        mvc.perform(get("/auth/session").session(fresh)).andExpect(jsonPath("$.data.provider").value(provider));
    }
    @Test void wrongStateNeverCallsProviderAndShowsReference() throws Exception {
        var start=mvc.perform(get("/oauth2/authorization/google")).andReturn();
        var s=(MockHttpSession)start.getRequest().getSession();
        mvc.perform(get("/login/oauth2/code/google").session(s).param("state","wrong").param("code","secret-code"))
                .andExpect(redirectedUrlPattern("/login.html?error=login_failed&reason=invalid_state_parameter&reference=*"));
        verifyNoInteractions(gateway);assertTrue(s.isInvalid());
    }
    @RestController static class Pages {
        @GetMapping({"/login.html","/res/test.js","/stores/list","/users"}) String page(){return "ok";}
        @PostMapping("/stores/uploadFile") String upload(){return "ok";}
    }
}
