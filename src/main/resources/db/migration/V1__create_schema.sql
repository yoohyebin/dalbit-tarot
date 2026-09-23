-- 달빛타로 초기 스키마

-- 관심 분야 마스터
CREATE TABLE category (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(20)  NOT NULL COMMENT '코드 (LOVE, MONEY 등)',
    label         VARCHAR(50)  NOT NULL COMMENT '화면 표시명',
    description   VARCHAR(200) NOT NULL COMMENT '카테고리 한 줄 설명',
    display_order INT          NOT NULL COMMENT '노출 순서',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 타로 카드 마스터 (78장)
CREATE TABLE tarot_card (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    code             VARCHAR(20)  NOT NULL COMMENT '코드 (MAJOR_00, CUPS_01 등)',
    name_ko          VARCHAR(50)  NOT NULL,
    name_en          VARCHAR(50)  NOT NULL,
    arcana_type      VARCHAR(10)  NOT NULL COMMENT 'MAJOR / MINOR',
    suit             VARCHAR(10)  NULL     COMMENT 'WANDS/CUPS/SWORDS/PENTACLES, 메이저는 NULL',
    card_number      INT          NOT NULL COMMENT '메이저 0~21, 마이너 1~14',
    upright_meaning  VARCHAR(255) NOT NULL,
    reversed_meaning VARCHAR(255) NOT NULL,
    keywords         VARCHAR(200) NOT NULL COMMENT '쉼표 구분 키워드',
    image_url        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tarot_card_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 1회 요청·결과 기록
CREATE TABLE reading_session (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    share_token     CHAR(22)    NOT NULL COMMENT '결과 공유 URL 토큰',
    category_id     BIGINT      NOT NULL,
    spread_type     VARCHAR(20) NOT NULL COMMENT 'ONE_CARD / THREE_CARD',
    birth_date      DATE        NULL     COMMENT '선택 입력',
    gender          VARCHAR(10) NULL     COMMENT '선택 입력 (MALE/FEMALE/NONE)',
    drawn_cards     JSON        NOT NULL COMMENT '[{position, cardId, reversed}]',
    llm_result_text TEXT        NULL,
    status          VARCHAR(20) NOT NULL COMMENT 'DRAWN/INTERPRETING/COMPLETED/FAILED',
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_reading_session_share_token (share_token),
    KEY idx_reading_session_created_at (created_at),
    CONSTRAINT fk_reading_session_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- LLM 호출 이력
CREATE TABLE request_log (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    session_id       BIGINT       NOT NULL,
    requested_at     DATETIME(6)  NOT NULL,
    response_time_ms INT          NULL,
    llm_model        VARCHAR(50)  NOT NULL,
    status           VARCHAR(20)  NOT NULL COMMENT 'SUCCESS/TIMEOUT/ERROR',
    error_message    VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_request_log_session_id (session_id),
    KEY idx_request_log_requested_at (requested_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
