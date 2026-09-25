package kr.dalbittarot.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    /** @return 삭제된 건수 */
    @Modifying
    @Query("delete from RequestLog l where l.requestedAt < :threshold")
    int deleteByRequestedAtBefore(LocalDateTime threshold);
}
