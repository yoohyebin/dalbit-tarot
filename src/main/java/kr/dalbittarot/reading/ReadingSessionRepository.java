package kr.dalbittarot.reading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {

    Optional<ReadingSession> findByShareToken(String shareToken);

    boolean existsByShareToken(String shareToken);
}
