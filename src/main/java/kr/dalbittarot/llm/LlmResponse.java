package kr.dalbittarot.llm;

/**
 * 모델이 만든 원문과 토큰 사용량.
 *
 * 토큰 수는 비용 계산에 쓴다. 제공사가 알려 주지 않으면(대역 등) null 이다.
 *
 * @param outputTokens 과금되는 출력 토큰. 추론(thinking) 토큰도 출력 단가로 과금되므로 합쳐서 담는다
 */
public record LlmResponse(String text, Integer promptTokens, Integer outputTokens) {

    public static LlmResponse textOnly(String text) {
        return new LlmResponse(text, null, null);
    }
}
