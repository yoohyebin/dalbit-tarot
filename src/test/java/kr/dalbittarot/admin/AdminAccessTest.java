package kr.dalbittarot.admin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "dalbit.rate-limit.enabled=false",
        "dalbit.admin.username=tester",
        "dalbit.admin.password=test-password"
})
@AutoConfigureMockMvc
@Transactional
class AdminAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("로그인하지 않으면 받은 의견 화면에 못 들어간다")
    void blocksAnonymousAccess() throws Exception {
        mockMvc.perform(get("/admin/feedback"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    @DisplayName("로그인 화면은 누구나 볼 수 있다")
    void allowsLoginPage() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/login"));
    }

    @Test
    @DisplayName("설정한 계정으로 로그인된다")
    void logsInWithConfiguredAccount() throws Exception {
        mockMvc.perform(formLogin("/admin/login").user("tester").password("test-password"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/admin/feedback"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인되지 않는다")
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(formLogin("/admin/login").user("tester").password("틀린비밀번호"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("로그인하면 받은 의견을 최신순으로 본다")
    void showsFeedbackWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/feedback").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"먼저\",\"content\":\"첫 번째 의견\"}"));
        mockMvc.perform(post("/api/feedback").contentType(APPLICATION_JSON)
                .content("{\"nickname\":\"나중\",\"content\":\"두 번째 의견\"}"));

        mockMvc.perform(get("/admin/feedback"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/feedback"))
                .andExpect(model().attributeExists("feedbacks"))
                .andExpect(content().string(org.hamcrest.Matchers.stringContainsInOrder(
                        "두 번째 의견", "첫 번째 의견")));
    }

    @Test
    @DisplayName("공개 화면은 로그인 없이 그대로 열린다")
    void keepsPublicPagesOpen() throws Exception {
        for (String path : new String[]{"/", "/reading/select", "/feedback", "/privacy"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("의견이 없으면 안내 문구를 보여준다")
    void showsEmptyNotice() throws Exception {
        mockMvc.perform(get("/admin/feedback"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("아직 받은 의견이 없습니다")));
    }
}
