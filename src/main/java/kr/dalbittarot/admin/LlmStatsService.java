package kr.dalbittarot.admin;

import kr.dalbittarot.reading.LlmCallStatus;
import kr.dalbittarot.reading.RequestLog;
import kr.dalbittarot.reading.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LLM 호출 기록(request_log)을 기간별로 집계한다.
 *
 * 30일치를 한 번에 읽어 메모리에서 나눈다. 해석 요청이 하루 수백 건 수준이라 충분하다.
 * 하루 수만 건이 넘어가면 DB 에서 GROUP BY 로 집계하도록 바꿔야 한다.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(LlmPricing.class)
public class LlmStatsService {

    /** InterpretationService 가 캐시 적중을 이 모델 이름으로 남긴다 */
    static final String CACHE_MODEL = "cache";

    private static final int MONTH_DAYS = 30;
    private static final int DAILY_DAYS = 14;
    private static final int RECENT_FAILURES = 10;

    private final RequestLogRepository requestLogRepository;
    private final LlmPricing pricing;

    @Transactional(readOnly = true)
    public LlmStats stats(LocalDate today) {
        // 오늘 끝까지로 막아 둔다. 서버 시계가 어긋나 '내일' 기록이 있어도 섞이지 않는다
        List<RequestLog> logs = requestLogRepository
                .findByRequestedAtGreaterThanEqualAndRequestedAtLessThanOrderByRequestedAtAsc(
                        today.minusDays(MONTH_DAYS - 1).atStartOfDay(), today.plusDays(1).atStartOfDay());

        List<LlmStats.Period> periods = List.of(
                new LlmStats.Period("오늘", summarize(since(logs, today))),
                new LlmStats.Period("최근 7일", summarize(since(logs, today.minusDays(6)))),
                new LlmStats.Period("최근 30일", summarize(logs)));

        Map<LocalDate, List<RequestLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getRequestedAt().toLocalDate()));
        List<LlmStats.Daily> daily = IntStream.range(0, DAILY_DAYS)
                .mapToObj(today::minusDays)
                .map(date -> new LlmStats.Daily(date, summarize(byDate.getOrDefault(date, List.of()))))
                .toList();

        List<LlmStats.Model> models = logs.stream()
                .collect(Collectors.groupingBy(RequestLog::getLlmModel, TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> new LlmStats.Model(entry.getKey(), summarize(entry.getValue())))
                .toList();

        List<LlmStats.Failure> failures = logs.stream()
                .filter(log -> log.getStatus() != LlmCallStatus.SUCCESS)
                .sorted(Comparator.comparing(RequestLog::getRequestedAt).reversed())
                .limit(RECENT_FAILURES)
                .map(log -> new LlmStats.Failure(
                        log.getRequestedAt(), log.getLlmModel(), log.getStatus(), log.getErrorMessage()))
                .toList();

        return new LlmStats(periods, daily, models, failures, pricing);
    }

    private List<RequestLog> since(List<RequestLog> logs, LocalDate from) {
        return logs.stream()
                .filter(log -> !log.getRequestedAt().toLocalDate().isBefore(from))
                .toList();
    }

    LlmStats.Summary summarize(List<RequestLog> logs) {
        int cacheHits = (int) logs.stream().filter(this::isCacheHit).count();
        int failures = (int) logs.stream().filter(log -> log.getStatus() != LlmCallStatus.SUCCESS).count();

        OptionalDouble avg = logs.stream()
                .filter(log -> log.getStatus() == LlmCallStatus.SUCCESS && !isCacheHit(log))
                .filter(log -> log.getResponseTimeMs() != null)
                .mapToInt(RequestLog::getResponseTimeMs)
                .average();

        long promptTokens = logs.stream().mapToLong(log -> orZero(log.getPromptTokens())).sum();
        long outputTokens = logs.stream().mapToLong(log -> orZero(log.getOutputTokens())).sum();
        double costUsd = pricing.costUsd(promptTokens, outputTokens);

        return new LlmStats.Summary(
                logs.size(), cacheHits, failures,
                avg.isPresent() ? (int) Math.round(avg.getAsDouble()) : null,
                promptTokens, outputTokens, costUsd, Math.round(costUsd * pricing.krwPerUsd()));
    }

    private boolean isCacheHit(RequestLog log) {
        return CACHE_MODEL.equals(log.getLlmModel());
    }

    private long orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
