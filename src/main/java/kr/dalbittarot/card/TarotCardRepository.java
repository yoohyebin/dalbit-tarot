package kr.dalbittarot.card;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarotCardRepository extends JpaRepository<TarotCard, Long> {

    List<TarotCard> findAllByIdIn(List<Long> ids);
}
