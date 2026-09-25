package kr.dalbittarot.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Gemini generateContent 연동.
 *
 * 키는 쿼리 파라미터(?key=) 대신 x-goog-api-key 헤더로 보낸다.
 * URL 에 실으면 액세스 로그·리퍼러에 키가 그대로 남는다.
 */
@Slf4j
public class GeminiLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties properties;

    public GeminiLlmClient(RestClient restClient, LlmProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        GenerateContentRequest request = GenerateContentRequest.of(
                systemPrompt, userPrompt, properties.maxOutputTokens(), properties.temperature());

        GenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(GenerateContentResponse.class);
        } catch (RestClientResponseException e) {
            // 본문에 키가 섞일 일은 없지만 응답 전문을 그대로 남기지는 않는다
            throw LlmException.error("Gemini 응답 상태 " + e.getStatusCode().value(), e);
        } catch (Exception e) {
            if (isTimeout(e)) {
                throw LlmException.timeout(e);
            }
            throw LlmException.error("Gemini 호출 실패: " + e.getClass().getSimpleName(), e);
        }

        return extractText(response);
    }

    @Override
    public String modelName() {
        return properties.model();
    }

    private boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.SocketTimeoutException || t instanceof java.net.http.HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw LlmException.error("Gemini 응답에 candidate 가 없다");
        }
        GenerateContentResponse.Candidate candidate = response.candidates().getFirst();

        // 안전 필터에 걸리거나 토큰 상한에 막히면 parts 가 비어서 온다
        if (candidate.content() == null || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw LlmException.error("Gemini 가 본문 없이 응답했다 (finishReason=" + candidate.finishReason() + ")");
        }
        String text = candidate.content().parts().getFirst().text();
        if (text == null || text.isBlank()) {
            throw LlmException.error("Gemini 응답 본문이 비어 있다");
        }
        if (!"STOP".equals(candidate.finishReason())) {
            log.warn("Gemini finishReason={} — 응답이 잘렸을 수 있다", candidate.finishReason());
        }
        return text;
    }

    // --- 요청/응답 매핑 ---

    record GenerateContentRequest(
            List<Content> contents,
            SystemInstruction systemInstruction,
            GenerationConfig generationConfig
    ) {
        static GenerateContentRequest of(String system, String user, int maxOutputTokens, double temperature) {
            return new GenerateContentRequest(
                    List.of(new Content("user", List.of(new Part(user)))),
                    new SystemInstruction(List.of(new Part(system))),
                    new GenerationConfig(maxOutputTokens, temperature)
            );
        }

        record Content(String role, List<Part> parts) {
        }

        record SystemInstruction(List<Part> parts) {
        }

        record Part(String text) {
        }

        record GenerationConfig(int maxOutputTokens, double temperature) {
        }
    }

    record GenerateContentResponse(List<Candidate> candidates, UsageMetadata usageMetadata) {

        record Candidate(Content content, String finishReason) {
        }

        record Content(List<Part> parts) {
        }

        record Part(String text) {
        }

        record UsageMetadata(Integer promptTokenCount, Integer candidatesTokenCount, Integer totalTokenCount) {
        }
    }
}
