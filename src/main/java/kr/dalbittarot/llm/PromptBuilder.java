package kr.dalbittarot.llm;

import kr.dalbittarot.reading.SpreadType;
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
 *
 * 해석은 카드를 한 장씩 짚어 가며 이야기하듯 풀게 한다. 요약문처럼 한 덩어리로 쓰면
 * "심심하다, 실제 타로를 보는 느낌이 없다"는 피드백이 있었다.
 * 분량은 전과 같게 두어 출력 토큰(비용)이 늘지 않게 한다.
 */
@Component
public class PromptBuilder {

    private static final String BASE_RULES = """
            당신은 차분하고 따뜻한 타로 해석가입니다.
            마주 앉은 사람에게 카드를 한 장씩 짚어 가며 이야기하듯 해석을 들려주세요.

            - 한국어로, 전체 400~500자 안팎으로 씁니다.
            - 문단 사이는 빈 줄 하나로 나눕니다. 제목, 번호, 기호, 굵은 글씨 같은 꾸밈은 쓰지 않습니다.
            - 카드마다 주어진 의미를 토대로 삼되, 그대로 옮겨 적지 말고 관심 분야에 맞게 풀어 쓰세요.
            - 의료·법률·투자에 대한 단정적 조언은 하지 않습니다.
            - 단정적인 예언이나 공포를 자극하는 표현은 쓰지 않습니다.
            - 이 결과가 참고용 콘텐츠임을 전제로 부드러운 어조를 유지하세요.
              다만 "참고용"이라는 안내는 화면에 따로 있으니 본문에 쓰지 않습니다.
            - 인사말 없이 해석 본문만 씁니다.""";

    private static final String ONE_CARD_STORY = """
            [이야기 순서]
            카드 한 장으로 세 문단을 씁니다.
            1. 카드에 담긴 장면이 무엇을 상징하는지 그려 보입니다.
            2. 그 상징이 지금 상황에 어떻게 닿는지 풉니다.
            3. 앞으로 해 볼 만한 것을 조언으로 건넵니다.""";

    private static final String THREE_CARD_STORY = """
            [이야기 순서]
            세 장은 각각 상황, 걸림돌, 조언 자리에 놓였습니다. 네 문단을 씁니다.
            1. 상황: 첫 번째 카드로 지금 놓인 상황을 그려 보입니다.
            2. 걸림돌: 두 번째 카드로 그 흐름을 막거나 흔드는 것을 짚습니다.
            3. 조언: 세 번째 카드로 어떻게 움직이면 좋을지 이야기합니다.
            4. 세 장을 한 흐름으로 묶어 한두 문장으로 마무리합니다.
            앞의 세 문단은 그 자리의 카드 이름으로 자연스럽게 시작하세요.""";

    public String systemPrompt(SpreadType spreadType) {
        String story = switch (spreadType) {
            case ONE_CARD -> ONE_CARD_STORY;
            case THREE_CARD -> THREE_CARD_STORY;
        };
        return BASE_RULES + "\n\n" + story;
    }

    public String userPrompt(String categoryLabel, LocalDate birthDate,
                             SpreadType spreadType, List<DrawnCardResponse> cards) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("[관심 분야] ").append(categoryLabel).append('\n');

        if (birthDate != null) {
            prompt.append("[생년월일 참고] ").append(birthDate)
                    .append(" (선택 입력, 실제 사주 계산에는 사용하지 않음)\n");
        }

        prompt.append("[뽑힌 카드]\n").append(formatCards(spreadType, cards)).append('\n');
        prompt.append("\n위 카드를 바탕으로 ").append(categoryLabel).append(" 해석을 작성해 주세요.");
        return prompt.toString();
    }

    private String formatCards(SpreadType spreadType, List<DrawnCardResponse> cards) {
        return cards.stream()
                .map(card -> "%d. [%s] %s(%s, %s) — %s".formatted(
                        card.position(),
                        spreadType.positionLabel(card.position()),
                        card.nameKo(),
                        card.nameEn(),
                        card.reversed() ? "역방향" : "정방향",
                        card.meaning()))
                .collect(Collectors.joining("\n"));
    }
}
