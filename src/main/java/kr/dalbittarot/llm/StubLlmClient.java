package kr.dalbittarot.llm;

import lombok.extern.slf4j.Slf4j;

/**
 * API 키 없이 로컬에서 화면·플로우를 돌려보기 위한 대역.
 *
 * 응답은 실제 해석과 같은 모양으로 둔다. 화면을 확인하거나 캡처할 때 안내 문구가 끼면
 * 실제 화면과 달라진다. 대신 운영에서는 이 대역으로 뜨지 않게 LlmConfig 가 막고,
 * 호출 기록의 모델명을 stub 으로 남겨 구분한다.
 */
@Slf4j
public class StubLlmClient implements LlmClient {

    private static final String MODEL_NAME = "stub";

    public StubLlmClient() {
        log.warn("dalbit.llm.api-key 가 비어 있어 StubLlmClient 로 뜬다. 실제 해석은 생성되지 않는다.");
    }

    @Override
    public LlmResponse generate(String systemPrompt, String userPrompt) {
        log.debug("Stub 해석 생성. userPrompt={}", userPrompt);
        return LlmResponse.textOnly("""
                뽑힌 카드는 지금의 흐름을 서둘러 밀어붙이기보다 한 박자 늦추라고 말합니다.
                당장 눈에 보이는 변화는 크지 않겠지만, 준비해 둔 것이 조용히 자리를 잡는 시기입니다.
                마음이 급해질 때마다 처음에 무엇을 바랐는지 떠올려 보세요.""");
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }
}
