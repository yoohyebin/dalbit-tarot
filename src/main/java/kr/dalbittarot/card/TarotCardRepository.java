package kr.dalbittarot.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TarotCardRepository extends JpaRepository<TarotCard, Long> {

    List<TarotCard> findAllByIdIn(List<Long> ids);

    /** 뽑기에는 id 만 필요하다. 78행을 통째로 읽지 않는다. */
    @Query("select c.id from TarotCard c")
    List<Long> findAllIds();
}
