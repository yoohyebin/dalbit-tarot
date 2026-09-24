package kr.dalbittarot.card;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 타로 카드 마스터(78장). 마이그레이션으로만 적재되며 애플리케이션에서는 읽기 전용이다.
 */
@Entity
@Table(name = "tarot_card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TarotCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name_ko", nullable = false, length = 50)
    private String nameKo;

    @Column(name = "name_en", nullable = false, length = 50)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "arcana_type", nullable = false, length = 10)
    private ArcanaType arcanaType;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Suit suit;

    @Column(name = "card_number", nullable = false)
    private int cardNumber;

    @Column(name = "upright_meaning", nullable = false)
    private String uprightMeaning;

    @Column(name = "reversed_meaning", nullable = false)
    private String reversedMeaning;

    @Column(nullable = false, length = 200)
    private String keywords;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    /** 뽑힌 방향에 해당하는 의미를 돌려준다. */
    public String meaningOf(boolean reversed) {
        return reversed ? reversedMeaning : uprightMeaning;
    }
}
