package kr.dalbittarot.reading;

import lombok.Getter;

import java.util.List;

@Getter
public enum SpreadType {

    ONE_CARD(1, "원카드", List.of("나의 카드")),
    // 카드마다 자리를 정해 두고 그 순서로 해석을 이야기처럼 풀어 간다
    THREE_CARD(3, "3카드", List.of("상황", "걸림돌", "조언"));

    private final int cardCount;
    private final String label;
    private final List<String> positionLabels;

    SpreadType(int cardCount, String label, List<String> positionLabels) {
        this.cardCount = cardCount;
        this.label = label;
        this.positionLabels = positionLabels;
    }

    /** 1부터 시작하는 자리 번호의 이름. 예) 3카드의 2번 자리는 "걸림돌" */
    public String positionLabel(int position) {
        return positionLabels.get(position - 1);
    }
}
