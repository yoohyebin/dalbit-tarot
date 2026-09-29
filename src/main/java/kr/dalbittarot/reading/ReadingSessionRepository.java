package kr.dalbittarot.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {

    Optional<ReadingSession> findByShareToken(String shareToken);

    boolean existsByShareToken(String shareToken);

    /** @return 삭제된 건수 */
    @Modifying
    @Query("delete from ReadingSession s where s.createdAt < :threshold")
    int deleteByCreatedAtBefore(LocalDateTime threshold);
}
