package kr.dalbittarot.common.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param enabled  테스트에서는 꺼 둔다
 * @param capacity window 안에 허용할 요청 수
 * @param window   창 길이
 */
@ConfigurationProperties(prefix = "dalbit.rate-limit")
public record RateLimitProperties(boolean enabled, int capacity, Duration window) {
}
