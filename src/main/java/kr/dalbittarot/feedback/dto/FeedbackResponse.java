package kr.dalbittarot.feedback.dto;

import kr.dalbittarot.feedback.Feedback;

import java.time.LocalDateTime;

public record FeedbackResponse(Long id, String nickname, String content, LocalDateTime createdAt) {

    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(), feedback.getNickname(), feedback.getContent(), feedback.getCreatedAt());
    }
}
