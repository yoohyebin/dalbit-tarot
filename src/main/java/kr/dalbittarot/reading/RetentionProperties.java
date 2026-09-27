package kr.dalbittarot.reading;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param period 보유 기간. 개인정보처리방침에 적은 기간과 반드시 같아야 한다
 * @param cron   삭제 작업 실행 주기
 */
@ConfigurationProperties(prefix = "dalbit.retention")
public record RetentionProperties(Duration period, String cron) {
}
