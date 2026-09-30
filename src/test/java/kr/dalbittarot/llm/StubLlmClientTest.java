package kr.dalbittarot.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubLlmClientTest {

    private final StubLlmClient client = new StubLlmClient();

    @Test
    @DisplayName("화면에서 실제 해석과 같은 모양으로 보이게 안내 문구를 넣지 않는다")
    void looksLikeARealReading() {
        String result = client.generate("시스템", "사용자").text();

        assertThat(result)
                .isNotBlank()
                .doesNotContain("예시 응답")
                .doesNotContain("API 키");
    }

    @Test
    @DisplayName("모델명을 stub 으로 남겨 비용 집계에서 구분된다")
    void reportsStubAsModelName() {
        assertThat(client.modelName()).isEqualTo("stub");
    }
}
