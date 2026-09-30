package kr.dalbittarot.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관리자 통계 화면의 추정 비용 계산용 단가. 모델을 바꾸면 같이 바꾼다.
 *
 * @param inputPerMillion  입력 100만 토큰당 달러
 * @param outputPerMillion 출력 100만 토큰당 달러. 추론(thinking) 토큰 포함
 * @param krwPerUsd        원화 환산용 환율. 대략적인 감을 잡는 용도라 고정값으로 둔다
 */
@ConfigurationProperties(prefix = "dalbit.llm.pricing")
public record LlmPricing(double inputPerMillion, double outputPerMillion, double krwPerUsd) {

    public double costUsd(long promptTokens, long outputTokens) {
        return (promptTokens * inputPerMillion + outputTokens * outputPerMillion) / 1_000_000;
    }
}
