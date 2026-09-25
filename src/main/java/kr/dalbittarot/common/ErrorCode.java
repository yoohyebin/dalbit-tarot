package kr.dalbittarot.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 클라이언트에 노출하는 오류 코드.
 * message 는 사용자에게 그대로 보여줄 수 있는 문구만 둔다. 내부 사정은 로그에만 남긴다.
 */
@Getter
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 관심 분야입니다."),
    READING_NOT_FOUND(HttpStatus.NOT_FOUND, "결과를 찾을 수 없습니다. 링크가 만료되었거나 잘못된 주소일 수 있습니다."),
    CARD_POOL_INSUFFICIENT(HttpStatus.INTERNAL_SERVER_ERROR, "카드를 뽑지 못했습니다. 잠시 후 다시 시도해 주세요."),
    INTERPRETATION_FAILED(HttpStatus.BAD_GATEWAY, "해석을 만들지 못했습니다. 잠시 후 다시 시도해 주세요."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
