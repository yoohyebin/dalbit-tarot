package kr.dalbittarot.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * LLM 연동 설정.
 *
 * @param apiKey          API 키. 비어 있으면 실제 호출 대신 StubLlmClient 가 뜬다
 * @param model           모델 이름
 * @param baseUrl         API 기본 주소
 * @param maxOutputTokens 응답 최대 토큰. 비용이 출력 토큰에 붙으므로 상한을 둔다
 * @param temperature     같은 카드 조합이라도 문장이 조금씩 달라지게 한다
 * @param timeout         호출 타임아웃
 * @param maxRetries      타임아웃·5xx 시 재시도 횟수
 */
@ConfigurationProperties(prefix = "dalbit.llm")
public record LlmProperties(
        String apiKey,
        String model,
        String baseUrl,
        int maxOutputTokens,
        double temperature,
        Duration timeout,
        int maxRetries
) {
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
