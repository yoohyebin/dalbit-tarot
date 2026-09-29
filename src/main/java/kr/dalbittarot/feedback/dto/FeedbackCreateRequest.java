package kr.dalbittarot.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param nickname 선택. 비우면 익명으로 저장된다
 * @param content  피드백 본문
 */
public record FeedbackCreateRequest(

        @Size(max = 20, message = "닉네임은 20자까지 쓸 수 있어요.")
        String nickname,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 1000, message = "내용은 1000자까지 쓸 수 있어요.")
        String content
) {
}
