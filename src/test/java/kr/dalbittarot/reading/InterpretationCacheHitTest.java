package kr.dalbittarot.reading;

import kr.dalbittarot.llm.LlmClient;
import kr.dalbittarot.llm.LlmResponse;
import kr.dalbittarot.reading.dto.ReadingCreateRequest;
import kr.dalbittarot.reading.dto.ReadingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 캐시가 실제로 LLM 호출을 막는지 본다.
 * 뽑기는 무작위라 같은 카드가 두 번 나오게 하려면 CardDrawer 를 고정해야 한다.
 */
@SpringBootTest(properties = {
        "dalbit.rate-limit.enabled=false",
        "dalbit.llm.max-retries=0"
})
@Transactional
class InterpretationCacheHitTest {

    private static final String INTERPRETATION = "캐시로 재사용될 해석";

    /** 캐시는 테스트 간 공유되는 싱글턴이다. 테스트마다 다른 카드를 써서 키가 겹치지 않게 한다. */
    private static final java.util.concurrent.atomic.AtomicLong CARD_ID_SEQUENCE =
            new java.util.concurrent.atomic.AtomicLong(1);

    @Autowired
    private ReadingService readingService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private LlmClient llmClient;

    @MockitoBean
    private CardDrawer cardDrawer;

    @BeforeEach
    void setUp() {
        given(llmClient.modelName()).willReturn("test-model");
        given(llmClient.generate(any(), any())).willReturn(LlmResponse.textOnly(INTERPRETATION));
        given(cardDrawer.draw(any(), any()))
                .willReturn(List.of(new DrawnCard(1, CARD_ID_SEQUENCE.getAndIncrement(), false)));
    }

    private String draw() {
        ReadingResponse reading = readingService.create(
                new ReadingCreateRequest("HEALTH", SpreadType.ONE_CARD, null, null));
        return reading.shareToken();
    }

    @Test
    @DisplayName("같은 카드·같은 분야를 다시 뽑으면 LLM 을 다시 부르지 않는다")
    void reusesInterpretationFromCache() {
        String first = draw();
        String second = draw();

        String firstText = interpretationService.interpret(first).interpretation();
        String secondText = interpretationService.interpret(second).interpretation();

        assertThat(firstText).isEqualTo(INTERPRETATION);
        assertThat(secondText).as("캐시에서 같은 문장이 나온다").isEqualTo(INTERPRETATION);
        verify(llmClient, times(1)).generate(any(), any());
    }

    @Test
    @DisplayName("캐시로 채운 건은 request_log 에 model=cache 로 남아 비용 집계에서 구분된다")
    void recordsCacheHitInRequestLog() {
        interpretationService.interpret(draw());
        String second = draw();

        interpretationService.interpret(second);

        Long sessionId = jdbcTemplate.queryForObject(
                "select id from reading_session where share_token = ?", Long.class, second);
        String model = jdbcTemplate.queryForObject(
                "select llm_model from request_log where session_id = ?", String.class, sessionId);
        assertThat(model).isEqualTo("cache");
    }
}
