package kr.dalbittarot.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterpretationPostProcessorTest {

    private static final int MAX_LENGTH = 700;

    private final InterpretationPostProcessor processor = new InterpretationPostProcessor();

    @Test
    @DisplayName("앞뒤 공백을 걷어낸다")
    void stripsSurroundingWhitespace() {
        assertThat(processor.process("  \n해석 본문입니다.\n  ")).isEqualTo("해석 본문입니다.");
    }

    @Test
    @DisplayName("빈 줄이 여러 개면 하나로 줄인다")
    void collapsesRepeatedBlankLines() {
        assertThat(processor.process("첫 문단.\n\n\n\n둘째 문단.")).isEqualTo("첫 문단.\n\n둘째 문단.");
    }

    @Test
    @DisplayName("줄 끝 공백을 없앤다")
    void removesTrailingSpacesOnEachLine() {
        assertThat(processor.process("첫 줄.   \n둘째 줄.")).isEqualTo("첫 줄.\n둘째 줄.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\n"})
    @DisplayName("빈 응답은 예외로 막는다")
    void rejectsBlankResponse(String raw) {
        assertThatThrownBy(() -> processor.process(raw)).isInstanceOf(LlmException.class);
    }

    @Test
    @DisplayName("null 도 예외로 막는다")
    void rejectsNull() {
        assertThatThrownBy(() -> processor.process(null)).isInstanceOf(LlmException.class);
    }

    @Test
    @DisplayName("상한을 넘으면 마지막 문장 끝에서 자른다")
    void truncatesAtSentenceBoundary() {
        String longText = "가".repeat(600) + ". " + "나".repeat(200) + ".";

        String result = processor.process(longText);

        assertThat(result).hasSizeLessThanOrEqualTo(MAX_LENGTH);
        assertThat(result).endsWith(".");
    }

    @Test
    @DisplayName("문장 부호가 없으면 말줄임표를 붙여 자른다")
    void truncatesWithEllipsisWhenNoSentenceEnd() {
        String result = processor.process("가".repeat(900));

        assertThat(result).hasSizeLessThanOrEqualTo(MAX_LENGTH + 1);
        assertThat(result).endsWith("…");
    }

    @Test
    @DisplayName("상한 이하면 그대로 둔다")
    void keepsShortTextAsIs() {
        String text = "짧은 해석입니다.";

        assertThat(processor.process(text)).isEqualTo(text);
    }
}
