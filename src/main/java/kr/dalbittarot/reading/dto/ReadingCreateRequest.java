package kr.dalbittarot.reading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import kr.dalbittarot.reading.Gender;
import kr.dalbittarot.reading.SpreadType;

import java.time.LocalDate;

/**
 * 카드 뽑기 요청.
 *
 * @param categoryCode 관심 분야 코드
 * @param spreadType   ONE_CARD / THREE_CARD
 * @param birthDate    선택. LLM 프롬프트 참고용이며 만세력 계산에는 쓰지 않는다
 * @param gender       선택. 없으면 NONE 으로 저장한다
 */
public record ReadingCreateRequest(

        @NotBlank(message = "관심 분야를 선택해 주세요.")
        String categoryCode,

        @NotNull(message = "스프레드를 선택해 주세요.")
        SpreadType spreadType,

        @Past(message = "생년월일은 오늘 이전 날짜여야 합니다.")
        LocalDate birthDate,

        Gender gender
) {
    public Gender genderOrNone() {
        return gender == null ? Gender.NONE : gender;
    }
}
