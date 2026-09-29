package kr.dalbittarot.reading;

import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * 카드 뽑기. 중복 없이 뽑고 각 카드의 정/역방향을 정한다.
 *
 * 부분 Fisher-Yates 로 필요한 장수만 섞는다. 78장을 통째로 섞을 이유가 없다.
 */
@Component
public class CardDrawer {

    private final RandomGenerator random;

    public CardDrawer() {
        this(new SecureRandom());
    }

    /** 테스트에서 고정 시드를 넣기 위한 생성자. */
    CardDrawer(RandomGenerator random) {
        this.random = random;
    }

    public List<DrawnCard> draw(List<Long> cardIds, SpreadType spreadType) {
        int count = spreadType.getCardCount();
        if (cardIds.size() < count) {
            throw new BusinessException(ErrorCode.CARD_POOL_INSUFFICIENT,
                    "카드 풀이 %d장뿐이라 %d장을 뽑을 수 없다".formatted(cardIds.size(), count));
        }

        List<Long> pool = new ArrayList<>(cardIds);
        List<DrawnCard> drawn = new ArrayList<>(count);
        for (int position = 0; position < count; position++) {
            int picked = position + random.nextInt(pool.size() - position);
            Collections.swap(pool, position, picked);
            drawn.add(new DrawnCard(position + 1, pool.get(position), random.nextBoolean()));
        }
        return drawn;
    }
}
