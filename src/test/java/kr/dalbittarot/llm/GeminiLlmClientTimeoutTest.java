package kr.dalbittarot.llm;

import com.sun.net.httpserver.HttpServer;
import kr.dalbittarot.reading.LlmCallStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 타임아웃 경로 검증.
 *
 * MockRestServiceServer 는 실제 소켓을 쓰지 않아 읽기 타임아웃을 재현할 수 없다.
 * 응답을 늦게 주는 HTTP 서버를 띄워 타임아웃이 실제로 걸리는지, 그리고 그 예외가
 * ERROR 가 아니라 TIMEOUT 으로 분류되는지 확인한다.
 */
class GeminiLlmClientTimeoutTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);
    private static final long SERVER_DELAY_MS = 3_000;

    private HttpServer server;
    private GeminiLlmClient client;

    @BeforeEach
    void startSlowServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(SERVER_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        LlmProperties properties = new LlmProperties(
                "test-key", "gemini-3.5-flash-lite", baseUrl, 700, 0.9, READ_TIMEOUT, 1);

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        factory.setReadTimeout(READ_TIMEOUT);

        client = new GeminiLlmClient(
                RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build(), properties);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    @DisplayName("응답이 늦으면 읽기 타임아웃에 걸린다")
    void timesOutOnSlowResponse() {
        long startedAt = System.nanoTime();

        assertThatThrownBy(() -> client.generate("시스템", "사용자"))
                .isInstanceOf(LlmException.class);

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        assertThat(elapsedMs)
                .as("서버가 %dms 를 끄는데 타임아웃 %dms 안에 끊겨야 한다", SERVER_DELAY_MS, READ_TIMEOUT.toMillis())
                .isLessThan(SERVER_DELAY_MS);
    }

    @Test
    @DisplayName("타임아웃은 ERROR 가 아니라 TIMEOUT 으로 분류한다")
    void classifiesAsTimeoutNotError() {
        assertThatThrownBy(() -> client.generate("시스템", "사용자"))
                .isInstanceOf(LlmException.class)
                .extracting("status")
                .as("request_log 의 status 로 남으므로 분류가 정확해야 한다")
                .isEqualTo(LlmCallStatus.TIMEOUT);
    }
}
