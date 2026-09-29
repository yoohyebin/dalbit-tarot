package kr.dalbittarot.reading;

import kr.dalbittarot.card.TarotCard;
import kr.dalbittarot.card.TarotCardRepository;
import kr.dalbittarot.category.Category;
import kr.dalbittarot.category.CategoryRepository;
import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.common.ErrorCode;
import kr.dalbittarot.reading.dto.DrawnCardResponse;
import kr.dalbittarot.reading.dto.ReadingCreateRequest;
import kr.dalbittarot.reading.dto.ReadingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReadingService {

    /** shareToken 은 128비트라 충돌이 사실상 없지만, UNIQUE 위반으로 요청이 죽는 것보다는 낫다. */
    private static final int TOKEN_RETRY_LIMIT = 3;

    private final CategoryRepository categoryRepository;
    private final TarotCardRepository tarotCardRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final CardDrawer cardDrawer;
    private final ShareTokenGenerator shareTokenGenerator;

    @Transactional
    public ReadingResponse create(ReadingCreateRequest request) {
        Category category = categoryRepository.findByCodeAndActiveTrue(request.categoryCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND,
                        "없는 카테고리 코드: " + request.categoryCode()));

        List<DrawnCard> drawnCards = cardDrawer.draw(tarotCardRepository.findAllIds(), request.spreadType());

        ReadingSession session = ReadingSession.draw(
                issueShareToken(),
                category,
                request.spreadType(),
                request.birthDate(),
                request.genderOrNone(),
                drawnCards
        );
        return toResponse(readingSessionRepository.save(session));
    }

    public ReadingResponse findByShareToken(String shareToken) {
        ReadingSession session = readingSessionRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.READING_NOT_FOUND,
                        "없는 shareToken: " + shareToken));
        return toResponse(session);
    }

    private String issueShareToken() {
        for (int attempt = 0; attempt < TOKEN_RETRY_LIMIT; attempt++) {
            String token = shareTokenGenerator.generate();
            if (!readingSessionRepository.existsByShareToken(token)) {
                return token;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "shareToken 을 %d회 시도했는데 전부 충돌했다".formatted(TOKEN_RETRY_LIMIT));
    }

    private ReadingResponse toResponse(ReadingSession session) {
        List<DrawnCard> drawnCards = session.getDrawnCards();
        Map<Long, TarotCard> cardsById = tarotCardRepository
                .findAllByIdIn(drawnCards.stream().map(DrawnCard::cardId).toList())
                .stream()
                .collect(Collectors.toMap(TarotCard::getId, Function.identity()));

        List<DrawnCardResponse> cards = drawnCards.stream()
                .sorted(Comparator.comparingInt(DrawnCard::position))
                .map(drawn -> DrawnCardResponse.of(
                        drawn.position(),
                        cardsById.get(drawn.cardId()),
                        drawn.reversed()))
                .toList();

        Category category = session.getCategory();
        return new ReadingResponse(
                session.getShareToken(),
                category.getCode(),
                category.getLabel(),
                session.getSpreadType(),
                cards,
                session.getLlmResultText(),
                session.getStatus(),
                session.getCreatedAt()
        );
    }
}
