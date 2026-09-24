-- 관심 분야 마스터 데이터

INSERT INTO category (code, label, description, display_order, active) VALUES
('LOVE',   '애정운', '관계의 흐름과 상대의 마음이 궁금할 때',        1, TRUE),
('MONEY',  '금전운', '재물의 들고 남, 돈에 관한 선택을 앞두었을 때',  2, TRUE),
('EXAM',   '합격운', '시험·취업 등 결과를 기다리고 있을 때',         3, TRUE),
('CAREER', '직장운', '일과 커리어의 방향을 정하고 싶을 때',          4, TRUE),
('HEALTH', '건강운', '몸과 마음의 상태를 돌아보고 싶을 때',          5, TRUE),
('TOTAL',  '총운',   '지금 나를 둘러싼 전체 흐름이 궁금할 때',        6, TRUE);
