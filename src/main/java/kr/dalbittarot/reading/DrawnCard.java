package kr.dalbittarot.reading;

/**
 * 뽑힌 카드 한 장. ReadingSession 의 JSON 컬럼에 저장된다.
 *
 * @param position 스프레드 내 자리 (1부터 시작)
 * @param cardId   tarot_card.id
 * @param reversed 역방향 여부
 */
public record DrawnCard(int position, Long cardId, boolean reversed) {
}
