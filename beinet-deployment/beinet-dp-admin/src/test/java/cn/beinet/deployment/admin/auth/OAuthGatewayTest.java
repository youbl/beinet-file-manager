package cn.beinet.deployment.admin.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.hamcrest.Matchers.containsString;

class OAuthGatewayTest {
    @Test void tokenExchangeUsesServerCredentialsPkceAndFixedCallback() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();var gateway=new OAuthGateway(builder.build());
        var p=new AuthProperties();p.getGoogle().setClientId("id");p.getGoogle().setClientSecret("secret");var client=new OAuthClients(p).require("google");
        server.expect(requestTo("https://oauth2.googleapis.com/token")).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("client_secret=secret")))
                .andExpect(content().string(containsString("code_verifier=verifier")))
                .andExpect(content().string(containsString("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Flogin%2Foauth2%2Fcode%2Fgoogle")))
                .andRespond(withSuccess("{\"access_token\":\"token\",\"token_type\":\"Bearer\"}",MediaType.APPLICATION_JSON));
        assertEquals("token",gateway.exchange(client,"code","verifier"));server.verify();
    }
    @Test void googleProfileUsesBearerHeaderAndRejectsEmptyTokenResponse() {
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();var gateway=new OAuthGateway(builder.build());
        var p=new AuthProperties();p.getGoogle().setClientId("id");p.getGoogle().setClientSecret("secret");var client=new OAuthClients(p).require("google");
        server.expect(requestTo(client.userInfoUri())).andExpect(header("Authorization","Bearer token"))
                .andRespond(withSuccess("{\"id\":\"g1\",\"email\":\"owner@example.com\",\"verified_email\":true}",MediaType.APPLICATION_JSON));
        server.expect(requestTo(client.tokenUri())).andRespond(withSuccess("{\"error\":\"invalid_client\",\"error_description\":\"secret\"}",MediaType.APPLICATION_JSON));
        assertEquals("g1",gateway.profile(client,"token").get("id"));
        var e=assertThrows(LoginFailure.class,()->gateway.exchange(client,"code","verifier"));
        assertEquals("invalid_token_response",e.reason());assertFalse(LoginDiagnostics.details(e).contains("secret"));server.verify();
    }
}
