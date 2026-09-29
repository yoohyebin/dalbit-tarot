package kr.dalbittarot.reading.dto;

import kr.dalbittarot.reading.ReadingStatus;
import kr.dalbittarot.reading.SpreadType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 뽑기 결과. 해석 전에는 interpretation 이 null 이고 status 가 DRAWN 이다.
 */
public record ReadingResponse(
        String shareToken,
        String categoryCode,
        String categoryLabel,
        SpreadType spreadType,
        List<DrawnCardResponse> cards,
        String interpretation,
        ReadingStatus status,
        LocalDateTime createdAt
) {
}
