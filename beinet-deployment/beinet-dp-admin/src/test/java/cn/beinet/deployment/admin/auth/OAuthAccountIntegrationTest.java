package cn.beinet.deployment.admin.auth;

import cn.beinet.business.login.service.OAuthAccountService;
import cn.beinet.business.login.service.UsersService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/** Real MyBatis + transaction integration; no external database or OAuth calls. */
@SpringBootTest(classes = OAuthAccountIntegrationTest.Application.class,
        properties = "spring.config.location=classpath:auth-test.yml",
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class OAuthAccountIntegrationTest {
    @Autowired
    OAuthAccountService accounts;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    UsersService users;

    @Test
    void sameIdentityReusesAccountButSameEmailAcrossProvidersDoesNotMerge() {
        Long google = accounts.login("google", "google-id", "owner@example.com", "Owner");
        assertEquals(google, accounts.login("google", "google-id", "new@example.com", "Owner"));
        Long github = accounts.login("github", "123", "new@example.com", "Owner");
        assertNotEquals(google, github);
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM oauth_identities", Integer.class));
        assertEquals(2, jdbc.queryForObject("SELECT login_count FROM users WHERE id=?", Integer.class, google));
    }

    @Test
    void disabledAndDeletedIdentitiesCannotSignInAgain() {
        Long id = accounts.login("google", "disabled", "owner@example.com", "Owner");
        jdbc.update("UPDATE users SET status=0 WHERE id=?", id);
        assertFalse(accounts.isEnabled(id));
        assertThrows(IllegalStateException.class, () -> accounts.login("google", "disabled", "owner@example.com", "Owner"));
        jdbc.update("UPDATE users SET status=1, delflag=1 WHERE id=?", id);
        assertFalse(accounts.isEnabled(id));
        assertThrows(IllegalStateException.class, () -> accounts.login("google", "disabled", "owner@example.com", "Owner"));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failedIdentityInsertRollsBackNewUser() {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> accounts.login("google", "x".repeat(256), "owner@example.com", "Owner"));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
    }

    @SpringBootConfiguration
    // User management and identity service use the same transaction/data model.
    @EnableAutoConfiguration
    @MapperScan("cn.beinet.business.login.dal")
    @Import({OAuthAccountService.class, UsersService.class})
    static class Application {
    }

    @Test
    void userManagementDeletionPreservesIdentityAndPreventsReregistration() {
        Long id = accounts.login("github", "deleted-through-admin", "owner@example.com", "Owner");
        assertTrue(users.removeById(id));
        assertFalse(accounts.isEnabled(id));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM oauth_identities WHERE user_id=?", Integer.class, id));
        assertThrows(IllegalStateException.class,
                () -> accounts.login("github", "deleted-through-admin", "owner@example.com", "Owner"));
        assertTrue(users.search(null).isEmpty());
    }
}
