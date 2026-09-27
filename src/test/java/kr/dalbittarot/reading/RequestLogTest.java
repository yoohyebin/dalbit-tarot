package kr.dalbittarot.reading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RequestLogTest {

    private static final int ERROR_MESSAGE_COLUMN_LENGTH = 500;

    @Test
    @DisplayName("성공 이력은 오류 메시지 없이 남는다")
    void recordsSuccessWithoutErrorMessage() {
        RequestLog log = RequestLog.success(1L, LocalDateTime.now(), 820, "gemini-2.5-flash-lite");

        assertThat(log.getStatus()).isEqualTo(LlmCallStatus.SUCCESS);
        assertThat(log.getErrorMessage()).isNull();
        assertThat(log.getResponseTimeMs()).isEqualTo(820);
    }

    @Test
    @DisplayName("컬럼 길이를 넘는 오류 메시지는 잘라서 저장한다")
    void truncatesLongErrorMessage() {
        String tooLong = "오".repeat(ERROR_MESSAGE_COLUMN_LENGTH + 200);

        RequestLog log = RequestLog.failure(1L, LocalDateTime.now(), null,
                "gemini-2.5-flash-lite", LlmCallStatus.ERROR, tooLong);

        assertThat(log.getErrorMessage())
                .as("자르지 않으면 INSERT 가 실패해 이력 자체가 안 남는다")
                .hasSize(ERROR_MESSAGE_COLUMN_LENGTH);
    }

    @Test
    @DisplayName("컬럼 길이 이내면 그대로 둔다")
    void keepsShortErrorMessage() {
        RequestLog log = RequestLog.failure(1L, LocalDateTime.now(), 300,
                "gemini-2.5-flash-lite", LlmCallStatus.TIMEOUT, "응답 시간 초과");

        assertThat(log.getErrorMessage()).isEqualTo("응답 시간 초과");
        assertThat(log.getStatus()).isEqualTo(LlmCallStatus.TIMEOUT);
    }

    @Test
    @DisplayName("오류 메시지가 없어도 이력은 남는다")
    void allowsNullErrorMessage() {
        RequestLog log = RequestLog.failure(1L, LocalDateTime.now(), null,
                "gemini-2.5-flash-lite", LlmCallStatus.ERROR, null);

        assertThat(log.getErrorMessage()).isNull();
    }
}
