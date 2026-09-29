package kr.dalbittarot.common;

import java.util.List;

/**
 * 공통 오류 응답.
 *
 * @param code    ErrorCode 이름
 * @param message 사용자에게 보여줄 문구
 * @param fields  필드 단위 검증 실패 내역. 해당 없으면 비운다
 */
public record ErrorResponse(String code, String message, List<FieldError> fields) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fields) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fields);
    }
}
