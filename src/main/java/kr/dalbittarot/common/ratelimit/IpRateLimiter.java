package kr.dalbittarot.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP 단위 고정 창 카운터.
 *
 * 창이 지나면 Caffeine 의 expireAfterWrite 로 항목이 사라지므로 따로 청소할 필요가 없다.
 * 단일 인스턴스 전제다. 2차 확장에서 인스턴스를 늘리면 Redis 같은 공용 저장소로 옮겨야 한다.
 */
@Component
public class IpRateLimiter {

    private static final int MAX_TRACKED_IPS = 10_000;

    private final RateLimitProperties properties;
    private final Cache<String, AtomicInteger> counters;

    public IpRateLimiter(RateLimitProperties properties) {
        this.properties = properties;
        this.counters = Caffeine.newBuilder()
                .maximumSize(MAX_TRACKED_IPS)
                .expireAfterWrite(properties.window())
                .build();
    }

    /** @return 허용되면 true, 한도를 넘었으면 false */
    public boolean tryAcquire(String clientIp) {
        if (!properties.enabled()) {
            return true;
        }
        AtomicInteger counter = counters.get(clientIp, ip -> new AtomicInteger());
        return counter.incrementAndGet() <= properties.capacity();
    }
}
