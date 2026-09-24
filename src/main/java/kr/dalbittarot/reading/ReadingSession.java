package kr.dalbittarot.reading;

import jakarta.persistence.*;
import kr.dalbittarot.category.Category;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 1회 요청·결과 기록. 회원 개념이 없으므로 shareToken 이 유일한 재접근 수단이다.
 */
@Entity
@Table(name = "reading_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_token", nullable = false, unique = true, length = 22)
    private String shareToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "spread_type", nullable = false, length = 20)
    private SpreadType spreadType;

    /** 선택 입력. LLM 프롬프트 참고용이며 만세력 계산에는 쓰지 않는다. */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drawn_cards", nullable = false)
    private List<DrawnCard> drawnCards;

    @Column(name = "llm_result_text", columnDefinition = "TEXT")
    private String llmResultText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReadingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ReadingSession(String shareToken, Category category, SpreadType spreadType,
                           LocalDate birthDate, Gender gender, List<DrawnCard> drawnCards) {
        this.shareToken = shareToken;
        this.category = category;
        this.spreadType = spreadType;
        this.birthDate = birthDate;
        this.gender = gender;
        this.drawnCards = drawnCards;
        this.status = ReadingStatus.DRAWN;
    }

    public static ReadingSession draw(String shareToken, Category category, SpreadType spreadType,
                                      LocalDate birthDate, Gender gender, List<DrawnCard> drawnCards) {
        return new ReadingSession(shareToken, category, spreadType, birthDate, gender, drawnCards);
    }

    public void markInterpreting() {
        this.status = ReadingStatus.INTERPRETING;
    }

    public void completeWith(String resultText) {
        this.llmResultText = resultText;
        this.status = ReadingStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = ReadingStatus.FAILED;
    }

    public boolean isInterpreted() {
        return status == ReadingStatus.COMPLETED;
    }
}
