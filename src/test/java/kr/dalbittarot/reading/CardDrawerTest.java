package kr.dalbittarot.reading;

import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.common.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardDrawerTest {

    private static final List<Long> FULL_POOL = IntStream.rangeClosed(1, 78)
            .mapToObj(Long::valueOf)
            .toList();

    private CardDrawer drawerWithSeed(long seed) {
        return new CardDrawer(new Random(seed));
    }

    @ParameterizedTest
    @EnumSource(SpreadType.class)
    @DisplayName("스프레드에 정의된 장수만큼 뽑는다")
    void drawsExactCardCount(SpreadType spreadType) {
        List<DrawnCard> drawn = drawerWithSeed(42).draw(FULL_POOL, spreadType);

        assertThat(drawn).hasSize(spreadType.getCardCount());
    }

    @Test
    @DisplayName("같은 카드를 두 번 뽑지 않는다")
    void neverDrawsDuplicates() {
        // 시드를 바꿔 가며 반복해야 특정 시드에서만 통과하는 걸 걸러낸다
        for (long seed = 0; seed < 500; seed++) {
            List<DrawnCard> drawn = drawerWithSeed(seed).draw(FULL_POOL, SpreadType.THREE_CARD);

            assertThat(drawn).extracting(DrawnCard::cardId).doesNotHaveDuplicates();
        }
    }

    @Test
    @DisplayName("자리 번호는 1부터 순서대로 매긴다")
    void assignsSequentialPositions() {
        List<DrawnCard> drawn = drawerWithSeed(42).draw(FULL_POOL, SpreadType.THREE_CARD);

        assertThat(drawn).extracting(DrawnCard::position).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("뽑힌 카드는 전부 풀 안에 있다")
    void drawsOnlyFromPool() {
        List<DrawnCard> drawn = drawerWithSeed(7).draw(FULL_POOL, SpreadType.THREE_CARD);

        assertThat(drawn).extracting(DrawnCard::cardId).allMatch(FULL_POOL::contains);
    }

    @Test
    @DisplayName("정방향과 역방향이 모두 나온다")
    void producesBothOrientations() {
        List<Boolean> orientations = IntStream.range(0, 200)
                .mapToObj(seed -> drawerWithSeed(seed).draw(FULL_POOL, SpreadType.ONE_CARD).getFirst().reversed())
                .toList();

        assertThat(orientations).contains(true, false);
    }

    @Test
    @DisplayName("풀이 뽑을 장수보다 적으면 예외를 던진다")
    void rejectsInsufficientPool() {
        List<Long> twoCards = List.of(1L, 2L);

        assertThatThrownBy(() -> drawerWithSeed(42).draw(twoCards, SpreadType.THREE_CARD))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CARD_POOL_INSUFFICIENT);
    }

    @Test
    @DisplayName("같은 시드면 같은 결과가 나온다")
    void isDeterministicForSameSeed() {
        List<DrawnCard> first = drawerWithSeed(99).draw(FULL_POOL, SpreadType.THREE_CARD);
        List<DrawnCard> second = drawerWithSeed(99).draw(FULL_POOL, SpreadType.THREE_CARD);

        assertThat(first).isEqualTo(second);
    }
}
