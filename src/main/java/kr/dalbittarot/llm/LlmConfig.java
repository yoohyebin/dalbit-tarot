package kr.dalbittarot.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 키가 없으면 대역으로 뜬다. 키 없이도 기동해야 화면 작업을 진행할 수 있다.
     *
     * 운영(prod)에서는 대역으로 뜨지 않고 기동을 멈춘다. 대역 응답은 고정 문장이라,
     * 키가 빠진 채로 뜨면 사용자에게 가짜 해석이 진짜처럼 나가도 알아챌 방법이 없다.
     * 기동이 실패하면 배포 헬스체크에서 바로 드러난다.
     */
    @Bean
    public LlmClient llmClient(LlmProperties properties, Environment environment) {
        if (!properties.hasApiKey()) {
            if (environment.matchesProfiles("prod")) {
                throw new IllegalStateException(
                        "운영 환경에 GEMINI_API_KEY 가 없다. 대역 응답으로 서비스하지 않도록 기동을 멈춘다.");
            }
            return new StubLlmClient();
        }
        return new GeminiLlmClient(geminiRestClient(properties), properties);
    }

    /**
     * 타임아웃을 반드시 건다. 걸지 않으면 LLM 이 늦을 때 요청 스레드가 무한정 붙잡힌다.
     */
    private RestClient geminiRestClient(LlmProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
