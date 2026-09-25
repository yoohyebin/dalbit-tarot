package kr.dalbittarot.reading;

import kr.dalbittarot.llm.LlmClient;
import kr.dalbittarot.llm.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dalbit.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Transactional
class InterpretationApiTest {

    private static final String INTERPRETATION = "별 카드는 서두르지 말라고 말합니다. 준비한 것이 자리를 잡는 시기입니다.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReadingSessionRepository readingSessionRepository;

    @MockitoBean
    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        given(llmClient.modelName()).willReturn("test-model");
    }

    /** 생년월일을 넣어 캐시를 타지 않게 한다. 캐시는 테스트 간 공유되는 싱글턴이다. */
    private String drawReading() throws Exception {
        String body = mockMvc.perform(post("/api/readings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"categoryCode":"LOVE","spreadType":"THREE_CARD","birthDate":"1996-05-12"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("shareToken").asText();
    }

    @Test
    @DisplayName("해석을 요청하면 본문이 채워지고 상태가 COMPLETED 로 바뀐다")
    void interpretsReading() throws Exception {
        given(llmClient.generate(any(), any())).willReturn(INTERPRETATION);
        String token = drawReading();

        mockMvc.perform(post("/api/readings/{shareToken}/interpretation", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareToken").value(token))
                .andExpect(jsonPath("$.interpretation").value(INTERPRETATION))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.cards.length()").value(3));
    }

    @Test
    @DisplayName("같은 결과를 두 번 요청해도 LLM 은 한 번만 부른다")
    void doesNotCallLlmTwiceForSameReading() throws Exception {
        given(llmClient.generate(any(), any())).willReturn(INTERPRETATION);
        String token = drawReading();

        mockMvc.perform(post("/api/readings/{shareToken}/interpretation", token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/readings/{shareToken}/interpretation", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interpretation").value(INTERPRETATION));

        verify(llmClient, times(1)).generate(any(), any());
    }

    @Test
    @DisplayName("첫 호출이 실패해도 재시도해서 성공하면 해석을 돌려준다")
    void retriesOnceThenSucceeds() throws Exception {
        given(llmClient.generate(any(), any()))
                .willThrow(LlmException.error("일시적 오류"))
                .willReturn(INTERPRETATION);
        String token = drawReading();

        mockMvc.perform(post("/api/readings/{shareToken}/interpretation", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interpretation").value(INTERPRETATION));

        verify(llmClient, times(2)).generate(any(), any());
    }

    @Test
    @DisplayName("재시도까지 실패하면 502 를 주고 세션을 FAILED 로 남긴다")
    void marksFailedWhenAllAttemptsFail() throws Exception {
        given(llmClient.generate(any(), any())).willThrow(LlmException.error("계속 실패"));
        String token = drawReading();

        mockMvc.perform(post("/api/readings/{shareToken}/interpretation", token))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("INTERPRETATION_FAILED"));

        assertThat(readingSessionRepository.findByShareToken(token))
                .get()
                .extracting(ReadingSession::getStatus)
                .isEqualTo(ReadingStatus.FAILED);
    }

    @Test
    @DisplayName("없는 공유 토큰으로 해석을 요청하면 404 다")
    void rejectsUnknownShareToken() throws Exception {
        mockMvc.perform(post("/api/readings/{shareToken}/interpretation", "AAAAAAAAAAAAAAAAAAAAAA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("READING_NOT_FOUND"));
    }
}
