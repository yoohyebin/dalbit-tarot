package kr.dalbittarot.admin;

import kr.dalbittarot.reading.LlmCallStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 통계 화면에 보여 줄 LLM 호출 집계.
 *
 * @param periods  오늘 · 최근 7일 · 최근 30일
 * @param daily    최근 14일, 오늘부터 거꾸로
 * @param models   최근 30일 모델별. 캐시 적중은 "cache", 로컬 대역은 "stub" 으로 따로 잡힌다
 * @param failures 최근 30일 실패 중 최신 몇 건
 */
public record LlmStats(
        List<Period> periods,
        List<Daily> daily,
        List<Model> models,
        List<Failure> failures,
        LlmPricing pricing
) {

    /**
     * @param calls         해석 요청 수. 재시도는 한 건으로 친다
     * @param cacheHits     모델을 부르지 않고 캐시로 답한 수
     * @param avgResponseMs 성공한 모델 호출의 평균 응답 시간. 캐시는 빼고 계산한다. 없으면 null
     */
    public record Summary(int calls, int cacheHits, int failures, Integer avgResponseMs,
                          long promptTokens, long outputTokens, double costUsd, long costKrw) {

        /** 실패율(%). 호출이 없으면 null */
        public Double failureRate() {
            return calls == 0 ? null : failures * 100.0 / calls;
        }
    }

    public record Period(String label, Summary summary) {
    }

    public record Daily(LocalDate date, Summary summary) {
    }

    public record Model(String name, Summary summary) {
    }

    public record Failure(LocalDateTime requestedAt, String model, LlmCallStatus status, String errorMessage) {
    }
}
