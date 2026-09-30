package kr.dalbittarot.llm;

/**
 * 해석 텍스트를 만들어주는 외부 모델.
 * 제공사를 갈아탈 수 있게 인터페이스로 끊어 둔다.
 */
public interface LlmClient {

    /**
     * @return 모델이 생성한 원문과 토큰 사용량. 후처리는 호출부에서 한다
     * @throws LlmException 타임아웃·오류·빈 응답
     */
    LlmResponse generate(String systemPrompt, String userPrompt);

    /** RequestLog 에 남길 모델 이름. */
    String modelName();
}
