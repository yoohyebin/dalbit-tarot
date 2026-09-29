package kr.dalbittarot.feedback;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dalbit.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Transactional
class FeedbackApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    @DisplayName("닉네임과 내용을 보내면 저장된다")
    void createsFeedback() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nickname":"별빛","content":"카드 넘기는 연출이 좋았어요."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("별빛"))
                .andExpect(jsonPath("$.content").value("카드 넘기는 연출이 좋았어요."))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("닉네임을 비우면 익명으로 저장한다")
    void storesAnonymousWhenNicknameBlank() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nickname":"   ","content":"닉네임 없이 보냅니다."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("익명"));
    }

    @Test
    @DisplayName("닉네임 항목이 아예 없어도 된다")
    void allowsMissingNickname() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"항목 자체를 안 보냈습니다."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nickname").value("익명"));
    }

    @Test
    @DisplayName("내용이 비면 400 이다")
    void rejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nickname":"별빛","content":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.fields[0].field").value("content"));
    }

    @Test
    @DisplayName("내용이 1000자를 넘으면 400 이다")
    void rejectsTooLongContent() throws Exception {
        String tooLong = "가".repeat(1001);

        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("content"));
    }

    @Test
    @DisplayName("닉네임이 20자를 넘으면 400 이다")
    void rejectsTooLongNickname() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("{\"nickname\":\"" + "가".repeat(21) + "\",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields[0].field").value("nickname"));
    }

    @Test
    @DisplayName("앞뒤 공백은 잘라서 저장한다")
    void stripsSurroundingWhitespace() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"nickname":"  별빛  ","content":"  공백이 붙은 내용  "}
                                """))
                .andExpect(status().isCreated());

        assertThat(feedbackRepository.findAll())
                .anySatisfy(saved -> {
                    assertThat(saved.getNickname()).isEqualTo("별빛");
                    assertThat(saved.getContent()).isEqualTo("공백이 붙은 내용");
                });
    }

    @Test
    @DisplayName("로그인 없이도 보낼 수 있다")
    void doesNotRequireLogin() throws Exception {
        mockMvc.perform(post("/api/feedback")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"content":"비로그인 사용자입니다."}
                                """))
                .andExpect(status().isCreated());
    }
}
