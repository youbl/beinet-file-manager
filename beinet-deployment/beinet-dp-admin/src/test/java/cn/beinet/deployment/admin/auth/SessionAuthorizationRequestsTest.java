package cn.beinet.deployment.admin.auth;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class SessionAuthorizationRequestsTest {
    @Test void stateProviderSessionAndReplayAreChecked() {
        var repo=new SessionAuthorizationRequests(); var req=new MockHttpServletRequest();
        var p=repo.start(req,"google","//evil.test");
        assertEquals("/stores/store.html",p.returnTo());
        assertThrows(LoginFailure.class,()->repo.consume(new MockHttpServletRequest(),"google",p.state()));
        assertThrows(LoginFailure.class,()->repo.consume(req,"github",p.state()));
        assertThrows(LoginFailure.class,()->repo.consume(req,"google","wrong"));
        assertEquals(p,repo.consume(req,"google",p.state()));
        assertThrows(LoginFailure.class,()->repo.consume(req,"google",p.state()));
    }
    @Test void pendingExpiresAndNewLoginReplacesOldOne() {
        var req=new MockHttpServletRequest();
        var repo=new SessionAuthorizationRequests(Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));
        var old=repo.start(req,"google",null);var current=repo.start(req,"github",null);
        assertThrows(LoginFailure.class,()->repo.consume(req,"google",old.state()));
        var later=new SessionAuthorizationRequests(Clock.fixed(Instant.EPOCH.plusSeconds(300),ZoneOffset.UTC));
        assertThrows(LoginFailure.class,()->later.consume(req,"github",current.state()));
    }
    @Test void concurrentCallbacksConsumeOnlyOnce() throws Exception {
        var repo=new SessionAuthorizationRequests();var req=new MockHttpServletRequest();var p=repo.start(req,"google",null);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> task=()->{try{repo.consume(req,"google",p.state());return true;}catch(LoginFailure e){return false;}};
            var results=pool.invokeAll(java.util.List.of(task,task));
            assertNotEquals(results.get(0).get(),results.get(1).get());
        }
    }
}
