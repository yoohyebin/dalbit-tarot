package kr.dalbittarot.reading;

import lombok.Getter;

@Getter
public enum SpreadType {

    ONE_CARD(1, "원카드"),
    THREE_CARD(3, "3카드");

    private final int cardCount;
    private final String label;

    SpreadType(int cardCount, String label) {
        this.cardCount = cardCount;
        this.label = label;
    }
}
