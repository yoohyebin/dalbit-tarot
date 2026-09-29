package kr.dalbittarot.reading;

import kr.dalbittarot.llm.LlmClient;
import kr.dalbittarot.reading.dto.ReadingCreateRequest;
import kr.dalbittarot.reading.dto.ReadingResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * 동시 요청 처리 확인.
 *
 * LLM 호출을 트랜잭션 밖으로 뺀 설계가 실제로 효과가 있는지 보는 테스트다.
 * 커넥션 풀을 3개로, 커넥션 대기 한도를 2초로 조여 두고 LLM 이 1.5초씩 끄는 상황에서
 * 동시 요청 10건을 던진다.
 *
 * 만약 LLM 호출이 트랜잭션 안에 있다면 요청 하나가 커넥션을 1.5초씩 붙잡아,
 * 3개를 넘어서는 요청부터 대기 한도 2초를 넘겨 터진다.
 * 트랜잭션 밖에 있으면 커넥션은 DB 작업 동안만 잡히므로 전부 통과한다.
 */
@SpringBootTest(properties = {
        "dalbit.rate-limit.enabled=false",
        "spring.datasource.hikari.maximum-pool-size=3",
        "spring.datasource.hikari.connection-timeout=2000"
})
class ConcurrentInterpretationTest {

    private static final int CONCURRENCY = 10;
    private static final long LLM_DELAY_MS = 1_500;
    private static final String INTERPRETATION = "동시 요청 테스트용 해석";

    @Autowired
    private ReadingService readingService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private ReadingSessionRepository readingSessionRepository;

    @MockitoBean
    private LlmClient llmClient;

    private final List<String> createdTokens = new ArrayList<>();

    @BeforeEach
    void setUp() {
        given(llmClient.modelName()).willReturn("slow-stub");
        given(llmClient.generate(any(), any())).willAnswer(invocation -> {
            Thread.sleep(LLM_DELAY_MS);
            return INTERPRETATION;
        });
    }

    @AfterEach
    void cleanUp() {
        // 동시성 테스트라 @Transactional 을 걸 수 없어 직접 지운다
        createdTokens.forEach(token ->
                readingSessionRepository.findByShareToken(token)
                        .ifPresent(readingSessionRepository::delete));
    }

    @Test
    @DisplayName("커넥션 풀보다 많은 동시 해석 요청을 모두 처리한다")
    void handlesMoreConcurrentRequestsThanConnections() throws Exception {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            ReadingResponse reading = readingService.create(
                    new ReadingCreateRequest("LOVE", SpreadType.ONE_CARD, null, null));
            tokens.add(reading.shareToken());
            createdTokens.add(reading.shareToken());
        }

        AtomicInteger succeeded = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch go = new CountDownLatch(1);

        long startedAt = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY)) {
            for (String token : tokens) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        go.await();
                        interpretationService.interpret(token);
                        succeeded.incrementAndGet();
                    } catch (Throwable t) {
                        failures.add(t);
                    }
                });
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();  // 한꺼번에 출발시킨다
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertThat(failures)
                .as("커넥션 풀이 말라 실패한 요청이 있으면 안 된다")
                .isEmpty();
        assertThat(succeeded.get()).isEqualTo(CONCURRENCY);
        assertThat(elapsedMs)
                .as("병렬로 처리돼야 한다. 직렬이면 %dms 는 걸린다", CONCURRENCY * LLM_DELAY_MS)
                .isLessThan(CONCURRENCY * LLM_DELAY_MS / 2);

        assertThat(tokens).allSatisfy(token ->
                assertThat(readingSessionRepository.findByShareToken(token)).get()
                        .extracting(ReadingSession::getStatus, ReadingSession::getLlmResultText)
                        .containsExactly(ReadingStatus.COMPLETED, INTERPRETATION));
    }
}
