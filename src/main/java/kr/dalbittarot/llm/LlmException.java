package kr.dalbittarot.llm;

import lombok.Getter;
import kr.dalbittarot.reading.LlmCallStatus;

@Getter
public class LlmException extends RuntimeException {

    private final LlmCallStatus status;

    public LlmException(LlmCallStatus status, String message) {
        super(message);
        this.status = status;
    }

    public LlmException(LlmCallStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public static LlmException timeout(Throwable cause) {
        return new LlmException(LlmCallStatus.TIMEOUT, "LLM 응답 시간을 초과했다", cause);
    }

    public static LlmException error(String message) {
        return new LlmException(LlmCallStatus.ERROR, message);
    }

    public static LlmException error(String message, Throwable cause) {
        return new LlmException(LlmCallStatus.ERROR, message, cause);
    }
}
