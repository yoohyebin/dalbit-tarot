-- LLM 호출마다 토큰 사용량을 남긴다. 관리자 통계 화면에서 비용을 계산하는 데 쓴다.
-- 이전 기록과 캐시 적중·대역 응답은 토큰을 모르므로 NULL 로 둔다.
ALTER TABLE request_log
    ADD COLUMN prompt_tokens INT NULL COMMENT '입력 토큰' AFTER llm_model,
    ADD COLUMN output_tokens INT NULL COMMENT '출력 토큰(추론 토큰 포함)' AFTER prompt_tokens;
