package kr.dalbittarot.reading;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 같은 입력에 대한 해석을 재사용해 LLM 호출을 줄인다.
 *
 * 캐싱 조건을 두 가지로 좁혔다.
 *
 * 1. 원카드만 — 3카드는 조합이 (78×2)³ 규모라 캐시가 맞을 일이 사실상 없다.
 *    원카드는 78장 × 정/역 × 카테고리 수라 156 × N 으로 유의미하게 겹친다.
 * 2. 생년월일을 입력하지 않은 요청만 — 생년월일이 프롬프트에 들어가므로,
 *    이걸 무시하고 캐싱하면 생일이 다른 사람에게 같은 문장이 나간다.
 */
@Component
public class InterpretationCache {

    private static final int MAX_ENTRIES = 1_000;
    private static final Duration TTL = Duration.ofHours(6);

    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(MAX_ENTRIES)
            .expireAfterWrite(TTL)
            .build();

    public Optional<String> find(InterpretationContext context) {
        return cacheable(context)
                ? Optional.ofNullable(cache.getIfPresent(key(context)))
                : Optional.empty();
    }

    public void put(InterpretationContext context, String interpretation) {
        if (cacheable(context)) {
            cache.put(key(context), interpretation);
        }
    }

    private boolean cacheable(InterpretationContext context) {
        return context.spreadType() == SpreadType.ONE_CARD && context.birthDate() == null;
    }

    private String key(InterpretationContext context) {
        DrawnCard card = context.drawnCards().getFirst();
        return "%s|%d|%b".formatted(context.categoryLabel(), card.cardId(), card.reversed());
    }
}
