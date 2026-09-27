package kr.dalbittarot.reading;

import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.reading.dto.ReadingCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * shareToken 은 128비트라 실제로 충돌하지 않는다.
 * 그래서 충돌 처리 분기는 생성기를 고정해야만 확인할 수 있다.
 */
@SpringBootTest(properties = "dalbit.rate-limit.enabled=false")
@Transactional
class ShareTokenCollisionTest {

    private static final String FIXED_TOKEN = "AAAAAAAAAAAAAAAAAAAAAA";
    private static final String NEXT_TOKEN = "BBBBBBBBBBBBBBBBBBBBBB";

    @Autowired
    private ReadingService readingService;

    @MockitoBean
    private ShareTokenGenerator shareTokenGenerator;

    private ReadingCreateRequest request() {
        return new ReadingCreateRequest("LOVE", SpreadType.ONE_CARD, null, null);
    }

    @Test
    @DisplayName("토큰이 겹치면 새로 발급해서 저장한다")
    void reissuesTokenOnCollision() {
        given(shareTokenGenerator.generate())
                .willReturn(FIXED_TOKEN)   // 첫 번째 뽑기가 선점
                .willReturn(FIXED_TOKEN)   // 두 번째 뽑기에서 충돌
                .willReturn(NEXT_TOKEN);   // 재발급

        readingService.create(request());
        var second = readingService.create(request());

        assertThat(second.shareToken()).isEqualTo(NEXT_TOKEN);
    }

    @Test
    @DisplayName("계속 겹치면 UNIQUE 위반 대신 명시적인 오류를 낸다")
    void failsExplicitlyWhenAlwaysColliding() {
        given(shareTokenGenerator.generate()).willReturn(FIXED_TOKEN);
        readingService.create(request());

        assertThatThrownBy(() -> readingService.create(request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("충돌");
    }
}
