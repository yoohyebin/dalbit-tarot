package kr.dalbittarot.llm;

import org.springframework.stereotype.Component;

/**
 * LLM 응답 후처리.
 *
 * 모델이 시스템 프롬프트를 늘 지키지는 않는다. 길이가 넘치거나 머리말이 붙어 오는 경우가 있어
 * DB·화면에 넣기 전에 한 번 다듬는다.
 */
@Component
public class InterpretationPostProcessor {

    private static final int MAX_LENGTH = 700;
    private static final String ELLIPSIS = "…";

    public String process(String raw) {
        if (raw == null || raw.isBlank()) {
            throw LlmException.error("후처리할 응답이 비어 있다");
        }

        String text = raw.strip()
                .replace("\r\n", "\n")
                .replaceAll("\n{3,}", "\n\n")   // 빈 줄이 여러 개면 하나로
                .replaceAll("[ \t]+\n", "\n");  // 줄 끝 공백

        return text.length() <= MAX_LENGTH ? text : truncate(text);
    }

    /** 문장 중간에서 끊기지 않게 마지막 문장 부호까지만 남긴다. */
    private String truncate(String text) {
        String cut = text.substring(0, MAX_LENGTH);
        int lastSentenceEnd = Math.max(cut.lastIndexOf('.'), Math.max(cut.lastIndexOf('!'), cut.lastIndexOf('?')));
        if (lastSentenceEnd > MAX_LENGTH / 2) {
            return cut.substring(0, lastSentenceEnd + 1);
        }
        return cut.strip() + ELLIPSIS;
    }
}
