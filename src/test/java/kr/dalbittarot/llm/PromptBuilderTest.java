package kr.dalbittarot.llm;

import kr.dalbittarot.reading.dto.DrawnCardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static kr.dalbittarot.reading.SpreadType.ONE_CARD;
import static kr.dalbittarot.reading.SpreadType.THREE_CARD;
import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    private DrawnCardResponse card(int position, String nameKo, boolean reversed) {
        return new DrawnCardResponse(position, "MAJOR_17", nameKo, "The Star",
                "/images/cards/major_17.webp", reversed, "희망과 조용한 치유");
    }

    @Test
    @DisplayName("시스템 프롬프트에 분량과 금지 사항을 명시한다")
    void systemPromptStatesConstraints() {
        String prompt = promptBuilder.systemPrompt(ONE_CARD);

        assertThat(prompt)
                .contains("400~500자")
                .contains("한국어")
                .contains("의료·법률·투자")
                .contains("참고용 콘텐츠");
    }

    @Test
    @DisplayName("3카드는 상황 → 걸림돌 → 조언 → 마무리 순서로 이야기하게 한다")
    void threeCardStoryFollowsPositions() {
        String prompt = promptBuilder.systemPrompt(THREE_CARD);

        assertThat(prompt).containsSubsequence("상황:", "걸림돌:", "조언:", "마무리");
    }

    @Test
    @DisplayName("원카드는 상징 → 지금 상황 → 조언 순서로 이야기하게 한다")
    void oneCardStoryFollowsSymbolThenAdvice() {
        String prompt = promptBuilder.systemPrompt(ONE_CARD);

        assertThat(prompt)
                .containsSubsequence("상징", "지금 상황", "조언")
                .doesNotContain("걸림돌");
    }

    @Test
    @DisplayName("관심 분야와 카드 정보를 프롬프트에 담는다")
    void userPromptContainsCategoryAndCards() {
        String prompt = promptBuilder.userPrompt("금전운", null, ONE_CARD, List.of(card(1, "별", false)));

        assertThat(prompt)
                .contains("[관심 분야] 금전운")
                .contains("별(The Star, 정방향)")
                .contains("희망과 조용한 치유")
                .contains("금전운 해석을 작성해 주세요");
    }

    @Test
    @DisplayName("역방향은 역방향으로 적는다")
    void marksReversedOrientation() {
        String prompt = promptBuilder.userPrompt("애정운", null, ONE_CARD, List.of(card(1, "별", true)));

        assertThat(prompt).contains("별(The Star, 역방향)");
    }

    @Test
    @DisplayName("생년월일을 넣으면 사주 계산에 쓰지 않는다는 단서를 함께 붙인다")
    void notesBirthDateIsReferenceOnly() {
        String prompt = promptBuilder.userPrompt("총운", LocalDate.of(1996, 5, 12), ONE_CARD,
                List.of(card(1, "별", false)));

        assertThat(prompt)
                .contains("[생년월일 참고] 1996-05-12")
                .contains("실제 사주 계산에는 사용하지 않음");
    }

    @Test
    @DisplayName("생년월일이 없으면 해당 줄을 아예 넣지 않는다")
    void omitsBirthDateLineWhenAbsent() {
        String prompt = promptBuilder.userPrompt("총운", null, ONE_CARD, List.of(card(1, "별", false)));

        assertThat(prompt).doesNotContain("생년월일");
    }

    @Test
    @DisplayName("3카드는 자리 이름을 붙여 순서대로 나열한다")
    void listsThreeCardsWithPositions() {
        String prompt = promptBuilder.userPrompt("합격운", null, THREE_CARD,
                List.of(card(1, "별", false), card(2, "달", true), card(3, "태양", false)));

        assertThat(prompt).containsSubsequence("1. [상황] 별", "2. [걸림돌] 달", "3. [조언] 태양");
    }
}
