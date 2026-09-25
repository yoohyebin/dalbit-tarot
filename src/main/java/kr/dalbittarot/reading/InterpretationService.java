package kr.dalbittarot.reading;

import kr.dalbittarot.common.BusinessException;
import kr.dalbittarot.common.ErrorCode;
import kr.dalbittarot.llm.InterpretationPostProcessor;
import kr.dalbittarot.llm.LlmClient;
import kr.dalbittarot.llm.LlmException;
import kr.dalbittarot.llm.LlmProperties;
import kr.dalbittarot.llm.PromptBuilder;
import kr.dalbittarot.reading.dto.ReadingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 해석 생성 흐름을 엮는다.
 *
 * 클래스에 @Transactional 을 걸지 않은 것은 의도다. LLM 호출을 트랜잭션 밖에 두려고
 * DB 작업을 InterpretationStore 로 떼어냈다 (그쪽 주석 참고).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterpretationService {

    private static final Duration RETRY_DELAY = Duration.ofMillis(500);
    private static final String CACHE_MODEL_NAME = "cache";

    private final InterpretationStore store;
    private final InterpretationCache cache;
    private final ReadingService readingService;
    private final PromptBuilder promptBuilder;
    private final InterpretationPostProcessor postProcessor;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;

    public ReadingResponse interpret(String shareToken) {
        InterpretationContext context = store.begin(shareToken);

        // 같은 결과 페이지를 새로고침해도 다시 과금되지 않게 한다
        if (context.alreadyInterpreted()) {
            log.debug("이미 해석된 세션이다. sessionId={}", context.sessionId());
            return readingService.findByShareToken(shareToken);
        }

        LocalDateTime requestedAt = LocalDateTime.now();
        Optional<String> cached = cache.find(context);
        if (cached.isPresent()) {
            store.complete(context.sessionId(), cached.get(), requestedAt, 0, CACHE_MODEL_NAME);
            return readingService.findByShareToken(shareToken);
        }

        String interpretation = callWithRetry(context, requestedAt);
        cache.put(context, interpretation);
        return readingService.findByShareToken(shareToken);
    }

    private String callWithRetry(InterpretationContext context, LocalDateTime requestedAt) {
        String systemPrompt = promptBuilder.systemPrompt();
        String userPrompt = promptBuilder.userPrompt(
                context.categoryLabel(), context.birthDate(), context.cards());

        int maxAttempts = llmProperties.maxRetries() + 1;
        LlmException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.nanoTime();
            try {
                String raw = llmClient.generate(systemPrompt, userPrompt);
                String interpretation = postProcessor.process(raw);
                int elapsedMs = elapsedMsSince(startedAt);

                store.complete(context.sessionId(), interpretation, requestedAt, elapsedMs, llmClient.modelName());
                log.info("해석 생성 완료. sessionId={} attempt={} elapsedMs={}",
                        context.sessionId(), attempt, elapsedMs);
                return interpretation;
            } catch (LlmException e) {
                lastFailure = e;
                log.warn("해석 생성 실패 ({}/{}). sessionId={} status={} reason={}",
                        attempt, maxAttempts, context.sessionId(), e.getStatus(), e.getMessage());
                if (attempt < maxAttempts) {
                    sleepBeforeRetry();
                }
            }
        }

        store.fail(context.sessionId(), lastFailure.getStatus(), lastFailure.getMessage(),
                requestedAt, null, llmClient.modelName());
        throw new BusinessException(ErrorCode.INTERPRETATION_FAILED, lastFailure.getMessage());
    }

    private int elapsedMsSince(long startedAtNanos) {
        return (int) ((System.nanoTime() - startedAtNanos) / 1_000_000);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw LlmException.error("재시도 대기 중 인터럽트", e);
        }
    }
}
