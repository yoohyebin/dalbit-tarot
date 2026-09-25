package kr.dalbittarot.reading;

import kr.dalbittarot.reading.dto.DrawnCardResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 프롬프트를 만드는 데 필요한 것만 담아 트랜잭션 밖으로 들고 나온다.
 * 엔티티를 그대로 내보내면 LLM 호출 중에 지연 로딩이 터진다.
 *
 * @param alreadyInterpreted 이미 해석이 끝난 세션이면 true. LLM 을 다시 부르지 않는다
 */
public record InterpretationContext(
        Long sessionId,
        String categoryLabel,
        SpreadType spreadType,
        LocalDate birthDate,
        List<DrawnCardResponse> cards,
        List<DrawnCard> drawnCards,
        boolean alreadyInterpreted
) {
}
