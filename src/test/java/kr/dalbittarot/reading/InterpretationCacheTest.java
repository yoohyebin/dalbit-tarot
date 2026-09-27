package kr.dalbittarot.reading;

import kr.dalbittarot.reading.dto.DrawnCardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretationCacheTest {

    private static final String TEXT = "캐시된 해석 문장";

    private InterpretationCache cache;

    @BeforeEach
    void setUp() {
        cache = new InterpretationCache();
    }

    private InterpretationContext context(SpreadType spreadType, LocalDate birthDate,
                                          long cardId, boolean reversed) {
        List<DrawnCard> drawn = spreadType == SpreadType.ONE_CARD
                ? List.of(new DrawnCard(1, cardId, reversed))
                : List.of(new DrawnCard(1, cardId, reversed),
                          new DrawnCard(2, cardId + 1, false),
                          new DrawnCard(3, cardId + 2, false));
        List<DrawnCardResponse> cards = drawn.stream()
                .map(d -> new DrawnCardResponse(d.position(), "CODE", "이름", "Name",
                        "/images/cards/x.webp", d.reversed(), "의미"))
                .toList();
        return new InterpretationContext(1L, "금전운", spreadType, birthDate, cards, drawn, false);
    }

    @Test
    @DisplayName("원카드 + 생년월일 미입력이면 캐시에 넣고 다시 꺼낸다")
    void cachesOneCardWithoutBirthDate() {
        InterpretationContext ctx = context(SpreadType.ONE_CARD, null, 10L, false);

        cache.put(ctx, TEXT);

        assertThat(cache.find(ctx)).contains(TEXT);
    }

    @Test
    @DisplayName("3카드는 캐시하지 않는다")
    void doesNotCacheThreeCard() {
        InterpretationContext ctx = context(SpreadType.THREE_CARD, null, 10L, false);

        cache.put(ctx, TEXT);

        assertThat(cache.find(ctx)).as("조합이 (78×2)³ 이라 캐시가 맞을 일이 없다").isEmpty();
    }

    @Test
    @DisplayName("생년월일이 있으면 캐시하지 않는다")
    void doesNotCacheWhenBirthDateGiven() {
        InterpretationContext ctx = context(SpreadType.ONE_CARD, LocalDate.of(1996, 5, 12), 10L, false);

        cache.put(ctx, TEXT);

        assertThat(cache.find(ctx))
                .as("생년월일이 프롬프트에 들어가므로 무시하고 캐싱하면 남의 해석이 나간다")
                .isEmpty();
    }

    @Test
    @DisplayName("카드가 다르면 다른 항목으로 취급한다")
    void separatesByCard() {
        cache.put(context(SpreadType.ONE_CARD, null, 10L, false), TEXT);

        assertThat(cache.find(context(SpreadType.ONE_CARD, null, 11L, false))).isEmpty();
    }

    @Test
    @DisplayName("정방향과 역방향은 다른 항목이다")
    void separatesByOrientation() {
        cache.put(context(SpreadType.ONE_CARD, null, 10L, false), TEXT);

        assertThat(cache.find(context(SpreadType.ONE_CARD, null, 10L, true))).isEmpty();
    }

    @Test
    @DisplayName("관심 분야가 다르면 다른 항목이다")
    void separatesByCategory() {
        InterpretationContext money = context(SpreadType.ONE_CARD, null, 10L, false);
        cache.put(money, TEXT);

        InterpretationContext love = new InterpretationContext(
                1L, "애정운", SpreadType.ONE_CARD, null, money.cards(), money.drawnCards(), false);

        assertThat(cache.find(love)).isEmpty();
    }
}
