package kr.dalbittarot.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmConfigTest {

    private final LlmConfig config = new LlmConfig();

    private LlmProperties properties(String apiKey) {
        return new LlmProperties(apiKey, "gemini-3.5-flash-lite", "https://llm.test/v1beta",
                700, 0.9, Duration.ofSeconds(20), 1);
    }

    @Test
    @DisplayName("로컬에서는 키가 없으면 대역으로 뜬다")
    void usesStubWithoutKeyLocally() {
        assertThat(config.llmClient(properties(""), new MockEnvironment()))
                .isInstanceOf(StubLlmClient.class);
    }

    @Test
    @DisplayName("운영에서는 키가 없으면 대역으로 뜨지 않고 기동을 멈춘다")
    void refusesStubInProduction() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThatThrownBy(() -> config.llmClient(properties(""), prod))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    @DisplayName("운영에서 키가 있으면 Gemini 로 뜬다")
    void usesGeminiWithKeyInProduction() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");

        assertThat(config.llmClient(properties("test-key"), prod))
                .isInstanceOf(GeminiLlmClient.class);
    }
}
