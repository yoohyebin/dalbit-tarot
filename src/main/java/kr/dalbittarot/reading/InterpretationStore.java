package kr.dalbittarot.reading;

import kr.dalbittarot.card.TarotCard;
import kr.dalbittarot.card.TarotCardRepository;
import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.common.ErrorCode;
import kr.dalbittarot.reading.dto.DrawnCardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 해석 과정의 DB 작업만 담당한다.
 *
 * LLM 호출은 10초 넘게 걸릴 수 있다. 그 호출을 트랜잭션 안에서 하면 커넥션을 그만큼 붙잡고 있어
 * 동시 요청이 몇 개만 늘어도 커넥션 풀이 마른다. 그래서 DB 작업을 이 클래스로 떼어내
 * '시작 → (트랜잭션 밖에서 LLM 호출) → 저장' 세 토막으로 나눈다.
 */
@Component
@RequiredArgsConstructor
public class InterpretationStore {

    private final ReadingSessionRepository readingSessionRepository;
    private final TarotCardRepository tarotCardRepository;
    private final RequestLogRepository requestLogRepository;

    @Transactional
    public InterpretationContext begin(String shareToken) {
        ReadingSession session = readingSessionRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_NOT_FOUND,
                        "없는 shareToken: " + shareToken));

        boolean done = session.isInterpreted();
        if (!done) {
            session.markInterpreting();
        }
        return new InterpretationContext(
                session.getId(),
                session.getCategory().getLabel(),
                session.getSpreadType(),
                session.getBirthDate(),
                toCardResponses(session.getDrawnCards()),
                session.getDrawnCards(),
                done
        );
    }

    @Transactional
    public void complete(Long sessionId, String interpretation, LocalDateTime requestedAt, int elapsedMs,
                         String model, Integer promptTokens, Integer outputTokens) {
        readingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_NOT_FOUND, "없는 sessionId: " + sessionId))
                .completeWith(interpretation);
        requestLogRepository.save(RequestLog.success(sessionId, requestedAt, elapsedMs, model, promptTokens, outputTokens));
    }

    @Transactional
    public void fail(Long sessionId, LlmCallStatus status, String message,
                     LocalDateTime requestedAt, Integer elapsedMs, String model) {
        readingSessionRepository.findById(sessionId).ifPresent(ReadingSession::markFailed);
        requestLogRepository.save(RequestLog.failure(sessionId, requestedAt, elapsedMs, model, status, message));
    }

    private List<DrawnCardResponse> toCardResponses(List<DrawnCard> drawnCards) {
        Map<Long, TarotCard> cardsById = tarotCardRepository
                .findAllByIdIn(drawnCards.stream().map(DrawnCard::cardId).toList())
                .stream()
                .collect(Collectors.toMap(TarotCard::getId, Function.identity()));

        return drawnCards.stream()
                .sorted(Comparator.comparingInt(DrawnCard::position))
                .map(drawn -> DrawnCardResponse.of(drawn.position(), cardsById.get(drawn.cardId()), drawn.reversed()))
                .toList();
    }
}
