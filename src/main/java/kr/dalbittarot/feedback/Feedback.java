package kr.dalbittarot.feedback;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 피드백. 로그인이 없으므로 작성자를 특정하지 않는다.
 * 닉네임은 표시용일 뿐이고 검증하지 않는다.
 */
@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback {

    public static final String ANONYMOUS = "익명";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 1000)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Feedback(String nickname, String content) {
        this.nickname = nickname;
        this.content = content;
    }

    public static Feedback of(String nickname, String content) {
        return new Feedback(blankToAnonymous(nickname), content.strip());
    }

    private static String blankToAnonymous(String nickname) {
        return nickname == null || nickname.isBlank() ? ANONYMOUS : nickname.strip();
    }
}
