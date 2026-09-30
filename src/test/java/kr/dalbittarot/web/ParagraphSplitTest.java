package kr.dalbittarot.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ParagraphSplitTest {

    @Test
    @DisplayName("빈 줄이든 줄바꿈 하나든 문단으로 나누고 앞뒤 공백을 지운다")
    void splitsOnLineBreaks() {
        assertThat(ReadingViewController.paragraphs("첫 문단\n\n  둘째 문단  \n셋째 문단\n"))
                .containsExactly("첫 문단", "둘째 문단", "셋째 문단");
    }

    @Test
    @DisplayName("해석이 아직 없으면 빈 목록이다")
    void emptyWhenNoInterpretation() {
        assertThat(ReadingViewController.paragraphs(null)).isEmpty();
    }
}
