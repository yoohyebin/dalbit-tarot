package kr.dalbittarot.reading;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 보유 기간이 지난 기록을 지운다.
 *
 * 개인정보처리방침에 '1년 후 자동 삭제'를 적어 두었으므로 이 작업이 실제로 돌아야 한다.
 * 방침에 적고 지우지 않으면 고지 내용이 사실과 달라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadingRetentionCleaner {

    private final ReadingSessionRepository readingSessionRepository;
    private final RequestLogRepository requestLogRepository;
    private final RetentionProperties properties;

    /** 트래픽이 적은 새벽에 하루 한 번. */
    @Scheduled(cron = "${dalbit.retention.cron}")
    @Transactional
    public void deleteExpired() {
        LocalDateTime threshold = LocalDateTime.now().minus(properties.period());

        int sessions = readingSessionRepository.deleteByCreatedAtBefore(threshold);
        int logs = requestLogRepository.deleteByRequestedAtBefore(threshold);

        if (sessions > 0 || logs > 0) {
            log.info("보유 기간 경과 기록 삭제. threshold={} sessions={} logs={}", threshold, sessions, logs);
        }
    }
}
