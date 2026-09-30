package kr.dalbittarot.reading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    /** 관리자 통계용. [from, to) 기간의 호출 기록을 오래된 순으로 */
    List<RequestLog> findByRequestedAtGreaterThanEqualAndRequestedAtLessThanOrderByRequestedAtAsc(
            LocalDateTime from, LocalDateTime to);

    /** @return 삭제된 건수 */
    @Modifying
    @Query("delete from RequestLog l where l.requestedAt < :threshold")
    int deleteByRequestedAtBefore(LocalDateTime threshold);
}
