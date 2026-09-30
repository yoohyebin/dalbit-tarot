package kr.dalbittarot.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiLlmClientTest {

    private static final String API_KEY = "test-api-key";

    private MockRestServiceServer server;
    private GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://llm.test/v1beta");
        server = MockRestServiceServer.bindTo(builder).build();
        LlmProperties properties = new LlmProperties(
                API_KEY, "gemini-3.5-flash-lite", "https://llm.test/v1beta",
                700, 0.9, Duration.ofSeconds(20), 1);
        client = new GeminiLlmClient(builder.build(), properties);
    }

    private void respondWith(String body) {
        server.expect(requestTo("https://llm.test/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("응답에서 해석 텍스트를 꺼낸다")
    void extractsGeneratedText() {
        respondWith("""
                {"candidates":[{"content":{"parts":[{"text":"별 카드는 희망을 말합니다."}]},
                 "finishReason":"STOP"}],
                 "usageMetadata":{"promptTokenCount":120,"candidatesTokenCount":80,"totalTokenCount":200}}
                """);

        String result = client.generate("시스템 프롬프트", "사용자 프롬프트").text();

        assertThat(result).isEqualTo("별 카드는 희망을 말합니다.");
        server.verify();
    }

    @Test
    @DisplayName("토큰 사용량을 꺼낸다. 추론 토큰은 출력 단가로 과금되므로 출력에 더한다")
    void extractsTokenUsageIncludingThoughts() {
        respondWith("""
                {"candidates":[{"content":{"parts":[{"text":"본문"}]},"finishReason":"STOP"}],
                 "usageMetadata":{"promptTokenCount":460,"candidatesTokenCount":252,
                                  "thoughtsTokenCount":30,"totalTokenCount":742}}
                """);

        LlmResponse response = client.generate("시스템", "사용자");

        assertThat(response.promptTokens()).isEqualTo(460);
        assertThat(response.outputTokens()).isEqualTo(282);
    }

    @Test
    @DisplayName("사용량이 없으면 토큰은 null 로 둔다")
    void leavesTokensNullWithoutUsage() {
        respondWith("""
                {"candidates":[{"content":{"parts":[{"text":"본문"}]},"finishReason":"STOP"}]}
                """);

        LlmResponse response = client.generate("시스템", "사용자");

        assertThat(response.promptTokens()).isNull();
        assertThat(response.outputTokens()).isNull();
    }

    @Test
    @DisplayName("키는 URL 이 아니라 헤더로 보낸다")
    void sendsApiKeyAsHeaderNotQueryParam() {
        server.expect(requestTo("https://llm.test/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"본문"}]},"finishReason":"STOP"}]}
                        """, MediaType.APPLICATION_JSON));

        client.generate("시스템", "사용자");

        // requestTo 가 쿼리스트링까지 일치를 요구하므로 ?key= 가 붙었다면 여기서 실패한다
        server.verify();
    }

    @Test
    @DisplayName("프롬프트를 시스템·사용자로 나눠 싣는다")
    void sendsSystemAndUserPromptSeparately() {
        server.expect(requestTo("https://llm.test/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("시스템 프롬프트"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("사용자 프롬프트"))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(700))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"본문"}]},"finishReason":"STOP"}]}
                        """, MediaType.APPLICATION_JSON));

        client.generate("시스템 프롬프트", "사용자 프롬프트");

        server.verify();
    }

    @Test
    @DisplayName("candidate 가 없으면 예외를 던진다")
    void failsWhenNoCandidate() {
        respondWith("{\"candidates\":[]}");

        assertThatThrownBy(() -> client.generate("시스템", "사용자"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("candidate");
    }

    @Test
    @DisplayName("안전 필터에 걸려 본문 없이 오면 예외를 던진다")
    void failsWhenBlockedWithoutContent() {
        respondWith("{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}");

        assertThatThrownBy(() -> client.generate("시스템", "사용자"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("SAFETY");
    }

    @Test
    @DisplayName("5xx 응답은 예외로 바꾸고 상태 코드만 남긴다")
    void wrapsServerError() {
        server.expect(requestTo("https://llm.test/v1beta/models/gemini-3.5-flash-lite:generateContent"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.generate("시스템", "사용자"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("토큰 상한에 걸려 잘렸어도 본문이 있으면 돌려준다")
    void returnsTruncatedTextWhenMaxTokensReached() {
        respondWith("""
                {"candidates":[{"content":{"parts":[{"text":"잘린 본문"}]},"finishReason":"MAX_TOKENS"}]}
                """);

        assertThat(client.generate("시스템", "사용자").text()).isEqualTo("잘린 본문");
    }
}
