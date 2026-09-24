package kr.dalbittarot.reading;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ShareTokenGeneratorTest {

    private final ShareTokenGenerator generator = new ShareTokenGenerator();

    @Test
    @DisplayName("share_token 컬럼 길이인 22자를 만든다")
    void generatesTokenOf22Characters() {
        assertThat(generator.generate()).hasSize(22);
    }

    @Test
    @DisplayName("URL 에 그대로 쓸 수 있는 문자만 쓴다")
    void generatesUrlSafeToken() {
        IntStream.range(0, 1_000)
                .forEach(i -> assertThat(generator.generate()).matches("[A-Za-z0-9_-]{22}"));
    }

    @Test
    @DisplayName("연속 생성해도 값이 겹치지 않는다")
    void generatesDistinctTokens() {
        Set<String> tokens = IntStream.range(0, 10_000)
                .mapToObj(i -> generator.generate())
                .collect(Collectors.toSet());

        assertThat(tokens).hasSize(10_000);
    }
}
