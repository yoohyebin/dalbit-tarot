package kr.dalbittarot.admin;

import kr.dalbittarot.reading.LlmCallStatus;
import kr.dalbittarot.reading.RequestLog;
import kr.dalbittarot.reading.RequestLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 다른 테스트가 커밋한 기록(동시성 테스트 등)은 오늘 날짜로 남는다.
 * 겹치지 않게 먼 과거를 "오늘"로 두고 집계한다.
 */
@SpringBootTest(properties = {
        "dalbit.llm.pricing.input-per-million=0.30",
        "dalbit.llm.pricing.output-per-million=2.50",
        "dalbit.llm.pricing.krw-per-usd=1400"
})
@Transactional
class LlmStatsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2001, 6, 30);
    private static final String GEMINI = "gemini-3.5-flash-lite";

    @Autowired
    private LlmStatsService statsService;

    @Autowired
    private RequestLogRepository requestLogRepository;

    private LocalDateTime at(int daysAgo, int hour) {
        return TODAY.minusDays(daysAgo).atTime(hour, 0);
    }

    @BeforeEach
    void setUp() {
        // 오늘: 성공 2, 캐시 적중 1, 실패 1
        requestLogRepository.save(RequestLog.success(1L, at(0, 9), 1_000, GEMINI, 400, 200));
        requestLogRepository.save(RequestLog.success(2L, at(0, 10), 3_000, GEMINI, 500, 300));
        requestLogRepository.save(RequestLog.success(3L, at(0, 11), 0, "cache", null, null));
        requestLogRepository.save(RequestLog.failure(4L, at(0, 12), null, GEMINI,
                LlmCallStatus.ERROR, "Gemini 응답 상태 404"));
        // 3일 전 성공, 20일 전 실패, 40일 전(30일 밖) 성공
        requestLogRepository.save(RequestLog.success(5L, at(3, 9), 2_000, GEMINI, 100, 100));
        requestLogRepository.save(RequestLog.failure(6L, at(20, 9), 20_000, GEMINI,
                LlmCallStatus.TIMEOUT, "응답 시간 초과"));
        requestLogRepository.save(RequestLog.success(7L, at(40, 9), 1_000, GEMINI, 9_999, 9_999));
    }

    @Test
    @DisplayName("오늘 집계: 캐시는 응답 시간 평균에서 빼고, 실패율은 전체 요청 기준이다")
    void summarizesToday() {
        LlmStats.Summary today = statsService.stats(TODAY).periods().get(0).summary();

        assertThat(today.calls()).isEqualTo(4);
        assertThat(today.cacheHits()).isEqualTo(1);
        assertThat(today.failures()).isEqualTo(1);
        assertThat(today.failureRate()).isEqualTo(25.0);
        assertThat(today.avgResponseMs()).isEqualTo(2_000);
        assertThat(today.promptTokens()).isEqualTo(900);
        assertThat(today.outputTokens()).isEqualTo(500);
        // (900 × 0.30 + 500 × 2.50) / 100만 = $0.00152 → 1400원 환산 2원
        assertThat(today.costUsd()).isCloseTo(0.00152, within(1e-9));
        assertThat(today.costKrw()).isEqualTo(2);
    }

    @Test
    @DisplayName("7일·30일 집계는 기간 안의 기록만 센다. 30일 밖은 빠진다")
    void summarizesPeriods() {
        LlmStats stats = statsService.stats(TODAY);

        LlmStats.Summary week = stats.periods().get(1).summary();
        assertThat(week.calls()).isEqualTo(5);
        assertThat(week.failures()).isEqualTo(1);

        LlmStats.Summary month = stats.periods().get(2).summary();
        assertThat(month.calls()).isEqualTo(6);
        assertThat(month.failures()).isEqualTo(2);
        assertThat(month.promptTokens()).isEqualTo(1_000);
    }

    @Test
    @DisplayName("일별은 오늘부터 14일을 빈 날까지 채워서 보여 준다")
    void fillsDailyRows() {
        LlmStats stats = statsService.stats(TODAY);

        assertThat(stats.daily()).hasSize(14);
        assertThat(stats.daily().get(0).date()).isEqualTo(TODAY);
        assertThat(stats.daily().get(0).summary().calls()).isEqualTo(4);
        assertThat(stats.daily().get(1).summary().calls()).isZero();
        assertThat(stats.daily().get(3).summary().calls()).isEqualTo(1);
    }

    @Test
    @DisplayName("모델별로 나누고, 실패는 최신순으로 보여 준다")
    void groupsByModelAndListsFailures() {
        LlmStats stats = statsService.stats(TODAY);

        assertThat(stats.models()).extracting(LlmStats.Model::name).containsExactly("cache", GEMINI);
        assertThat(stats.models().get(1).summary().calls()).isEqualTo(5);

        assertThat(stats.failures()).extracting(LlmStats.Failure::status)
                .containsExactly(LlmCallStatus.ERROR, LlmCallStatus.TIMEOUT);
        assertThat(stats.failures().get(0).errorMessage()).isEqualTo("Gemini 응답 상태 404");
    }
}
