package kr.dalbittarot.reading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReadingRetentionCleanerTest {

    @Autowired
    private ReadingRetentionCleaner cleaner;

    @Autowired
    private ReadingSessionRepository readingSessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** created_at 은 @CreationTimestamp 라 JPA 로는 과거 시각을 넣을 수 없다. */
    private void insertSession(String shareToken, LocalDateTime createdAt) {
        Long categoryId = jdbcTemplate.queryForObject(
                "select id from category where code = 'LOVE'", Long.class);
        jdbcTemplate.update("""
                insert into reading_session
                    (share_token, category_id, spread_type, drawn_cards, status, created_at, updated_at)
                values (?, ?, 'ONE_CARD', '[{"position":1,"cardId":1,"reversed":false}]', 'DRAWN', ?, ?)
                """, shareToken, categoryId, createdAt, createdAt);
    }

    private void insertRequestLog(Long sessionId, LocalDateTime requestedAt) {
        jdbcTemplate.update("""
                insert into request_log (session_id, requested_at, llm_model, status)
                values (?, ?, 'test-model', 'SUCCESS')
                """, sessionId, requestedAt);
    }

    @Test
    @DisplayName("보유 기간이 지난 세션은 지우고 최근 것은 남긴다")
    void deletesOnlyExpiredSessions() {
        LocalDateTime longAgo = LocalDateTime.now().minusDays(400);
        LocalDateTime recent = LocalDateTime.now().minusDays(10);
        insertSession("EXPIRED_______________".substring(0, 22), longAgo);
        insertSession("RECENT________________".substring(0, 22), recent);

        cleaner.deleteExpired();

        assertThat(readingSessionRepository.findByShareToken("EXPIRED_______________".substring(0, 22)))
                .as("400일 전 세션은 삭제된다").isEmpty();
        assertThat(readingSessionRepository.findByShareToken("RECENT________________".substring(0, 22)))
                .as("10일 전 세션은 남는다").isPresent();
    }

    @Test
    @DisplayName("보유 기간이 지난 요청 이력도 함께 지운다")
    void deletesExpiredRequestLogs() {
        insertSession("LOGOWNER______________".substring(0, 22), LocalDateTime.now().minusDays(10));
        Long sessionId = jdbcTemplate.queryForObject(
                "select id from reading_session where share_token = ?", Long.class,
                "LOGOWNER______________".substring(0, 22));
        insertRequestLog(sessionId, LocalDateTime.now().minusDays(400));
        insertRequestLog(sessionId, LocalDateTime.now().minusDays(5));

        cleaner.deleteExpired();

        Integer remaining = jdbcTemplate.queryForObject(
                "select count(*) from request_log where session_id = ?", Integer.class, sessionId);
        assertThat(remaining).as("최근 이력 1건만 남는다").isEqualTo(1);
    }
}
