package kr.dalbittarot.llm;

import kr.dalbittarot.reading.dto.DrawnCardResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 프롬프트 조립.
 *
 * 사용자가 자유 입력으로 넣는 값이 없다. 카테고리는 DB 코드, 생년월일은 LocalDate,
 * 성별은 enum, 카드는 서버가 뽑은 것이라 프롬프트 인젝션 경로가 없다.
 * 나중에 자유 입력(질문 직접 쓰기 등)을 받게 되면 이 전제가 깨지니 그때 이스케이프를 넣어야 한다.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT = """
            당신은 친절하고 신뢰감 있는 타로 해석가입니다.
            아래 정보를 참고하여 300~500자 내외의 한국어로 해석을 작성하세요.

            - 의료·법률·투자에 대한 단정적 조언은 하지 않습니다.
            - 이 결과가 참고용 콘텐츠임을 전제로 부드러운 어조를 유지하세요.
            - 카드마다 주어진 의미를 토대로 삼되, 그대로 옮겨 적지 말고 관심 분야에 맞게 풀어 쓰세요.
            - 단정적인 예언이나 공포를 자극하는 표현은 쓰지 않습니다.
            - 머리말·맺음말 없이 해석 본문만 작성하세요.""";

    public String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String userPrompt(String categoryLabel, LocalDate birthDate, List<DrawnCardResponse> cards) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("[관심 분야] ").append(categoryLabel).append('\n');

        if (birthDate != null) {
            prompt.append("[생년월일 참고] ").append(birthDate)
                    .append(" (선택 입력, 실제 사주 계산에는 사용하지 않음)\n");
        }

        prompt.append("[뽑힌 카드]\n").append(formatCards(cards)).append('\n');
        prompt.append("\n위 카드 조합을 바탕으로 ").append(categoryLabel).append(" 해석을 작성해 주세요.");
        return prompt.toString();
    }

    private String formatCards(List<DrawnCardResponse> cards) {
        return cards.stream()
                .map(card -> "%d. %s(%s, %s) — %s".formatted(
                        card.position(),
                        card.nameKo(),
                        card.nameEn(),
                        card.reversed() ? "역방향" : "정방향",
                        card.meaning()))
                .collect(Collectors.joining("\n"));
    }
}
