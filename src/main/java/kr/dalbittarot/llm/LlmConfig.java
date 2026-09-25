package kr.dalbittarot.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
     */
    @Bean
    public LlmClient llmClient(LlmProperties properties) {
        if (!properties.hasApiKey()) {
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
