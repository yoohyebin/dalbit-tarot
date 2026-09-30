package kr.dalbittarot.reading;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 호출 이력. 비용·응답시간 모니터링과 장애 분석에 쓴다.
 * 개인정보는 남기지 않고 세션 참조만 보관한다.
 */
@Entity
@Table(name = "request_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "llm_model", nullable = false, length = 50)
    private String llmModel;

    /** 모르면 null. 캐시 적중·대역 응답·이전 기록 */
    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    /** 추론 토큰 포함. 모르면 null */
    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LlmCallStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    private RequestLog(Long sessionId, LocalDateTime requestedAt, Integer responseTimeMs, String llmModel,
                       Integer promptTokens, Integer outputTokens, LlmCallStatus status, String errorMessage) {
        this.sessionId = sessionId;
        this.requestedAt = requestedAt;
        this.responseTimeMs = responseTimeMs;
        this.llmModel = llmModel;
        this.promptTokens = promptTokens;
        this.outputTokens = outputTokens;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public static RequestLog success(Long sessionId, LocalDateTime requestedAt, int responseTimeMs, String llmModel,
                                     Integer promptTokens, Integer outputTokens) {
        return new RequestLog(sessionId, requestedAt, responseTimeMs, llmModel,
                promptTokens, outputTokens, LlmCallStatus.SUCCESS, null);
    }

    public static RequestLog failure(Long sessionId, LocalDateTime requestedAt, Integer responseTimeMs,
                                     String llmModel, LlmCallStatus status, String errorMessage) {
        return new RequestLog(sessionId, requestedAt, responseTimeMs, llmModel,
                null, null, status, truncate(errorMessage));
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
