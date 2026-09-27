# 달빛타로

타로 카드를 뽑으면 LLM이 관심 분야에 맞춰 해석해주는 웹 서비스.
회원가입·결제 없이 바로 이용하는 무료 서비스입니다.

## 서비스 플로우

메인 → 관심 분야 선택 → 정보 입력(선택) → 스프레드 선택 → 카드 뽑기 → LLM 해석 → 결과 → (선택) 공유

- 생년월일·성별은 **선택 입력**이며, LLM 프롬프트 참고용으로만 사용합니다. 만세력 계산은 하지 않습니다.
- 결과는 `shareToken` 기반 URL로 다시 접근할 수 있습니다.

## 기술 스택

| 구분 | 기술 |
|---|---|
| 언어 | Java 21 (LTS) |
| 프레임워크 | Spring Boot 4.1.1 |
| 빌드 | Gradle 9.7.1 (Wrapper) |
| 웹/화면 | Spring Web MVC + Thymeleaf |
| 데이터 | Spring Data JPA + MySQL 8.0 |
| 마이그레이션 | Flyway |
| 배포 | Docker, GitHub Actions, AWS EC2 |

> Spring Boot 4.x부터 스타터 이름이 바뀌었습니다: `spring-boot-starter-web` → `spring-boot-starter-webmvc`,
> 테스트는 스타터별 `-test` 아티팩트(`spring-boot-starter-webmvc-test` 등)를 씁니다.

## 로컬 실행

사전 요구사항: JDK 21, Docker Desktop

```bash
docker compose up -d
./gradlew bootRun
```

기동 확인: http://localhost:8080/actuator/health → `{"status":"UP"}`

### LLM API 키

키 없이도 기동된다. 이때는 `StubLlmClient` 가 떠서 예시 문장을 돌려주므로 화면 작업은 가능하다.
실제 해석을 보려면 [Google AI Studio](https://aistudio.google.com/apikey) 에서 키를 받아 넣는다.

```bash
export GEMINI_API_KEY=발급받은_키
./gradlew bootRun
```

무료 티어는 입력이 모델 학습에 쓰인다. 생년월일을 받는 서비스이므로 **오픈 전에 유료 티어로 전환**해야 한다.

### 로컬 DB

| 항목 | 값 |
|---|---|
| 호스트 포트 | **3307** (컨테이너 내부는 3306) |
| DB | `dalbit_tarot` |
| 계정 | `dalbit` / `dalbit` |

> **포트가 3307인 이유**: 로컬에 Homebrew MySQL이 127.0.0.1:3306을 점유하고 있으면
> JDBC가 컨테이너 대신 그쪽으로 붙어 `Access denied` 가 납니다. 충돌을 피하려고 3307로 옮겼습니다.
> Homebrew MySQL을 쓰지 않는다면 `brew services stop mysql` 후 3306으로 되돌려도 됩니다.

스키마는 Flyway(`src/main/resources/db/migration`)가 관리하며, JPA는 `ddl-auto: validate`로 검증만 합니다.

### 테스트

테스트가 실제 MySQL에 붙으므로 `docker compose up -d` 가 먼저 실행돼 있어야 합니다.

```bash
./gradlew test
```

## 개발 단계

- [x] 1. 개발 환경 세팅 (로컬 기동 확인 완료)
- [x] 2. 데이터 모델 설계 및 DB 구축 (카드 78장 시드)
- [x] 3. 백엔드 코어 API (카드 뽑기, 세션 저장)
- [x] 4. LLM 연동 (프롬프트, 호출, 후처리)
- [x] 5. 화면 개발 (Thymeleaf, 반응형)
- [ ] 6. 테스트
- [ ] 7. AWS 배포
- [ ] 8. 운영 및 모니터링

## 카드 이미지 출처

Rider-Waite-Smith 타로 덱(1909) — 퍼블릭 도메인.
그림 작가 Pamela Colman Smith(1951년 사망) 기준 국내 저작재산권 보호기간(사후 50년)이 2001년 만료되었습니다.
U.S. Games Systems의 1971년 리컬러판 및 카드 뒷면·패키지 디자인은 별도 저작권이 있으므로 사용하지 않습니다.

## 안내

본 서비스의 해석 결과는 참고용 콘텐츠이며, 의료·법률·투자 등에 대한 조언이 아닙니다.
