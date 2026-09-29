package kr.dalbittarot.reading.dto;

import kr.dalbittarot.card.TarotCard;

public record DrawnCardResponse(
        int position,
        String code,
        String nameKo,
        String nameEn,
        String imageUrl,
        boolean reversed,
        String meaning
) {
    public static DrawnCardResponse of(int position, TarotCard card, boolean reversed) {
        return new DrawnCardResponse(
                position,
                card.getCode(),
                card.getNameKo(),
                card.getNameEn(),
                card.getImageUrl(),
                reversed,
                card.meaningOf(reversed)
        );
    }
}
