-- 사용자 피드백

CREATE TABLE feedback (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    nickname   VARCHAR(20)  NOT NULL COMMENT '미입력 시 익명으로 저장',
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_feedback_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
