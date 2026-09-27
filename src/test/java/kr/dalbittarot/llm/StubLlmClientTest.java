package kr.dalbittarot.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubLlmClientTest {

    private final StubLlmClient client = new StubLlmClient();

    @Test
    @DisplayName("실제 해석으로 오인되지 않게 대역임을 밝힌다")
    void statesThatItIsNotARealReading() {
        String result = client.generate("시스템", "사용자");

        assertThat(result)
                .contains("예시 응답")
                .contains("실제 해석이 아닙니다");
    }

    @Test
    @DisplayName("모델명을 stub 으로 남겨 비용 집계에서 구분된다")
    void reportsStubAsModelName() {
        assertThat(client.modelName()).isEqualTo("stub");
    }
}
