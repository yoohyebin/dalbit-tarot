package kr.dalbittarot.feedback;

import kr.dalbittarot.feedback.dto.FeedbackCreateRequest;
import kr.dalbittarot.feedback.dto.FeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    public static final int PAGE_SIZE = 20;

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public FeedbackResponse create(FeedbackCreateRequest request) {
        Feedback saved = feedbackRepository.save(Feedback.of(request.nickname(), request.content()));
        log.info("피드백 등록. id={} nickname={}", saved.getId(), saved.getNickname());
        return FeedbackResponse.from(saved);
    }

    public Page<FeedbackResponse> findPage(int page) {
        return feedbackRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), PAGE_SIZE))
                .map(FeedbackResponse::from);
    }
}
