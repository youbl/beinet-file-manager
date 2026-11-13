package cn.beinet.deployment.admin.auth;

import cn.beinet.deployment.admin.AdminApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AdminApplication.class, properties = {
        "spring.config.location=classpath:auth-test.yml",
        "spring.datasource.url=jdbc:h2:mem:beinet-smoke;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "beinet.config.path=target/auth-smoke-missing.ini",
        "jasypt.encryptor.password=isolated-test-only"
})
@AutoConfigureMockMvc
class AuthApplicationSmokeTest {
    @Autowired
    MockMvc mvc;

    @Test
    void actualApplicationServesVueLoginAndProtectsStaticBusinessPage() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("new Vue")));
        mvc.perform(get("/res/vue.min.js")).andExpect(status().isOk());
        mvc.perform(get("/stores/store.html")).andExpect(status().isFound());
        mvc.perform(get("/stores/list")).andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/providers")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
