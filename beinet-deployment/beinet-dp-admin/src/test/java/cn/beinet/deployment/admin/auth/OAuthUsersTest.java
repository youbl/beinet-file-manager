package cn.beinet.deployment.admin.auth;
import cn.beinet.business.login.service.OAuthAccountService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class OAuthUsersTest {
    AuthProperties properties=new AuthProperties();
    OAuthAccountService accounts=mock(OAuthAccountService.class);
    OAuthUsers users=new OAuthUsers(properties,accounts);
    @Test void emptyAndWildcardListsDenyWhileAdminsAutomaticallyQualify() {
        assertFalse(properties.allows("owner@example.com"));properties.setAllowedEmails(List.of("*@example.com"));
        assertFalse(properties.allows("owner@example.com"));properties.setAdminEmails(List.of(" OWNER@example.com "));
        assertTrue(properties.allows("owner@example.com"));assertTrue(properties.isAdmin("owner@example.com"));
        assertFalse(properties.allows("owner+other@example.com"));
    }
    @Test void googleRequiresVerifiedAllowlistedEmailBeforeDatabaseWrite() {
        var data=new HashMap<String,Object>(Map.of("id","g1","email","owner@example.com","verified_email",true));
        assertEquals("email_not_allowed",assertThrows(LoginFailure.class,()->users.google(data)).reason());
        properties.setAdminEmails(List.of("owner@example.com"));data.put("verified_email",false);
        assertEquals("email_not_verified",assertThrows(LoginFailure.class,()->users.google(data)).reason());
        verifyNoInteractions(accounts);data.put("verified_email",true);
        when(accounts.login(eq("google"),eq("g1"),eq("owner@example.com"),any())).thenReturn(1L);
        assertEquals(1L,users.google(data).id());
    }
    @Test void githubSupportsVerifiedPrivateAddressAndPrefersPrimary() {
        properties.setAllowedEmails(List.of("private@example.com","primary@example.com"));
        when(accounts.login(any(),any(),any(),any())).thenReturn(2L);
        var profile=Map.<String,Object>of("id",123,"name","user");
        assertThrows(LoginFailure.class,()->users.github(profile,List.of(Map.of("email","private@example.com","verified",false))));
        var user=users.github(profile,List.of(Map.of("email","private@example.com","verified",true),
                Map.of("email","primary@example.com","verified",true,"primary",true)));
        assertEquals("primary@example.com",user.email());assertEquals("123",user.subject());
    }
    @Test void databaseFailureKeepsReasonAndIdentityButNotSecretMessage() {
        properties.setAllowedEmails(List.of("owner@example.com"));
        when(accounts.login(any(),any(),any(),any())).thenThrow(new IllegalStateException("internal secret"));
        var e=assertThrows(LoginFailure.class,()->users.google(Map.of("id","g1","email","owner@example.com","verified_email",true)));
        assertEquals("account_storage_failed",e.reason());assertTrue(e.context().contains("owner@example.com"));
        assertFalse(LoginDiagnostics.details(e).contains("internal secret"));
    }
}
