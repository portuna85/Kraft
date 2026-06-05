# KRAFT Lotto — 로또 번호 조합 분석 도구

로또 6/45 과거 당첨 데이터 기반 편향 회피 번호 조합 도구입니다.  
당첨 확률을 예측하거나 높인다고 주장하지 않습니다.  
모든 6개 번호 조합의 1등 당첨 확률은 동일합니다.

> 서비스 주소: [www.kraft.io.kr](https://www.kraft.io.kr)

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| 언어 / 런타임 | Java 25 |
| 프레임워크 | Spring Boot 4, Spring MVC, Spring Data JPA |
| 템플릿 | Thymeleaf 3, HTMX |
| 프론트엔드 | Bootstrap, 정적 CSS/JS |
| DB (운영) | MariaDB 11.7 |
| DB (테스트) | H2, Testcontainers |
| 캐시 | Caffeine |
| 스케줄링 | ShedLock (JDBC) |
| 빌드 | Gradle (Kotlin DSL) |
| 테스트 | JUnit 5, Playwright E2E, JaCoCo, Checkstyle, SpotBugs |
| 모니터링 | Micrometer, Prometheus |

---

## 주요 기능

| 페이지 | 설명 |
|--------|------|
| `/` | 편향 회피 번호 추천 (홀짝·합산 필터, 규칙 on/off) |
| `/latest` | 최신 회차 당첨번호, 세후 수령액 계산 |
| `/rounds` | 전체 회차 조회·검색 |
| `/frequency` | 번호별 과거 출현 빈도 (기간 필터) |
| `/stats` | 홀짝·합산 패턴 통계 + 이론적 조합 비율 비교 |
| `/analysis` | 직접 입력한 조합의 과거 당첨 이력·빈도 분석 |
| `/companion` | 동반 출현 기록 조회 |
| `/news` | 로또 관련 뉴스 (비복권 은유 표현 자동 필터) |
| `/methodology` | 추천 알고리즘 공개 |
| `/data-source` | 데이터 출처 및 수집 방식 안내 |
| `/faq` | 자주 묻는 질문 |
| `/responsible-play` | 책임 있는 복권 이용 안내 |

---

## 편향 회피 규칙

번호 생성 시 아래 규칙에 해당하는 조합을 제외합니다. 각 규칙은 고급 설정에서 개별로 on/off 할 수 있습니다.

| 규칙 | 제외 기준 |
|------|-----------|
| BirthdayBiasRule | 6개 번호 전부 31 이하 (생일 편향) |
| LongRunRule | 5개 이상 연속 번호 포함 |
| ArithmeticSequenceRule | 완전 등차수열 (예: 1,8,15,22,29,36) |
| SingleDecadeRule | 동일 십의 자리에 5개 이상 집중 |
| PastWinningRule | 과거 1등 당첨 조합과 완전 일치 |

---

## 로컬 실행

### 사전 준비

- Java 25
- Docker, Docker Compose

### 환경 파일 생성

```bash
cp .env.local.example .env
```

`.env`의 DB 접속 정보를 수정한 뒤 MariaDB 컨테이너를 실행합니다.

```bash
docker compose -f docker-compose.local.yml up -d
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

| 주소 | 설명 |
|------|------|
| `http://localhost:8080` | 공개 화면 |
| `http://localhost:8080/actuator/health` | 헬스 체크 |
| `http://localhost:8080/swagger-ui/index.html` | Swagger UI (local 프로파일) |

> 로컬에서는 `KRAFT_API_CLIENT=mock`으로 설정하면 외부 API 호출 없이 개발할 수 있습니다.

---

## 테스트

```bash
# 전체 테스트
./gradlew test

# 정적 분석 포함
./gradlew check

# 엄격 모드 (커버리지 + 정적 분석 임계값 적용)
./gradlew check -PstrictStatic=true -PstrictCoverage=true

# E2E (Playwright)
npm ci && npx playwright install chromium && npm run test:e2e
```

---

## 프로젝트 구조

```
src/main/java/com/kraft/lotto
├── feature/
│   ├── recommend/      # 편향 회피 번호 추천
│   ├── statistics/     # 빈도·패턴·이론 분포 통계
│   ├── winningnumber/  # 당첨번호 수집·조회
│   └── news/           # 뉴스 수집·필터링
├── infra/              # 보안·캐시·DB·스케줄 설정
├── support/            # 공통 필터·예외·IP 제한
└── web/                # 공개 컨트롤러, 운영 API
```

---

## 주요 환경 변수

| 변수 | 설명 |
|------|------|
| `SPRING_PROFILES_ACTIVE` | `local` / `prod` |
| `KRAFT_DB_HOST` | DB 호스트 |
| `KRAFT_DB_USER` | DB 사용자 |
| `KRAFT_DB_PASSWORD` | DB 비밀번호 |
| `KRAFT_API_CLIENT` | `mock` / `smok` / `real` |
| `KRAFT_COLLECT_AUTO_ENABLED` | 자동 수집 on/off |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | 운영 API 토큰 |
| `KRAFT_NEWS_EXCLUDE_KEYWORDS` | 뉴스 제외 키워드 목록 |
