<div align="center">

# kLo — 한국 로또 6/45 번호 추천

통계 기반 편향 제거 규칙을 적용한 번호 조합 추천 · 당첨 번호 수집 · 빈도 통계

[![CI](https://github.com/portuna85/kLo/actions/workflows/ci.yml/badge.svg)](https://github.com/portuna85/kLo/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-007396?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.x-6DB33F?style=flat-square&logo=springboot)
![MariaDB](https://img.shields.io/badge/MariaDB-11.7-003545?style=flat-square&logo=mariadb)

**[www.kraft.io.kr](https://www.kraft.io.kr)**

</div>

---

## 주요 기능

| 기능 | 설명 |
|---|---|
| **번호 추천** | 생일 편향·등차수열·단일 십의 자리·장기 미출현·과거 당첨 조합 등 5가지 제외 규칙을 통과한 조합 추천. 확률 향상이 아닌 인기 편향 조합 회피로 공동 당첨 시 분배 인원을 줄이는 것이 목적 |
| **당첨 번호 수집** | 동행복권 API에서 회차별 당첨 번호를 자동(토·일) 또는 수동(Ops API)으로 수집 |
| **번호 빈도 통계** | 전체 회차 누적 번호 출현 빈도 요약 및 시각화 |
| **회차 검색** | 특정 회차 당첨 번호 및 1등 당첨 정보 조회 |
| **Ops 관리 대시보드** | 수집 트리거·서킷브레이커 상태·fetch 로그·실패 이유 분석·추천 메트릭 |

---

## 기술 스택

| 분류 | 내용 |
|---|---|
| **런타임** | Java 25 · Spring Boot 4.0.x · Virtual Threads |
| **데이터베이스** | MariaDB 11.7 *(운영)* · H2 *(테스트)* |
| **스키마 관리** | Flyway |
| **캐시** | Caffeine |
| **분산 스케줄러** | ShedLock + JDBC |
| **프론트엔드** | Thymeleaf · HTMX · Bootstrap 5.3 · 글라스모피즘 CSS · 라이트/다크 테마 |
| **PWA** | Web App Manifest · safe-area-inset · 홈 화면 추가 지원 |
| **관찰 가능성** | Micrometer · Prometheus · Grafana · Micrometer Tracing (OTLP) |
| **컨테이너** | Docker Compose (`observability` 프로파일로 Grafana/Prometheus 선택 실행) |
| **테스트** | JUnit 5 · jqwik (property-based) · Testcontainers · Playwright (E2E) |
| **정적 분석** | Checkstyle · SpotBugs · JaCoCo |
| **접근성** | WCAG 2 AA 준수 (axe-core E2E 검증) |

---

## 아키텍처

```
com.kraft.lotto
├── feature/
│   ├── winningnumber/   당첨 번호 수집·조회 (API 클라이언트, 스케줄러, 서킷브레이커)
│   ├── recommend/       번호 추천 엔진 (ExclusionRule 전략 패턴)
│   └── statistics/      번호 빈도 통계 요약
├── infra/config/        프로퍼티 바인딩, 시작 시 검증
├── support/             공통: 필터(Rate Limit, Ops/Actuator 접근, 보안 헤더), 예외 처리
└── web/                 홈/분석/통계 컨트롤러, Ops API/페이지 컨트롤러
```

| 컴포넌트 | 설명 |
|---|---|
| `ExclusionRule` | 추천 후보 필터링 전략 인터페이스. Spring이 구현체를 자동 수집해 고정 순서로 평가 |
| `ApiCircuitBreaker` | 동행복권 API 호출을 감싸는 직접 구현 서킷브레이커 (`closed → open → half-open`) |
| `LottoCollectionCommandService` | `CollectionRunState` 뮤텍스 + ShedLock으로 중복 수집 방지 |
| `PastWinningCache` | 시작 시 전체 당첨 이력을 메모리에 로드하여 추천 성능 확보 |
| `ApiCircuitBreakerRegistry` | Micrometer Gauge로 서킷브레이커 상태를 Prometheus에 노출 |

---

## 프로파일

| 프로파일 | DB 호스트 | API 클라이언트 | 자동 수집 |
|---|---|---|:---:|
| **`local`** *(기본값)* | `localhost` | `smok` *(프로필 기본값, `.env.local.example`는 `mock`으로 오버라이드)* | — |
| **`prod`** | `mariadb` *(컨테이너)* | real / smok | ✓ |

---

## 빠른 시작

### 사전 요건

- Java 25+
- Docker + Docker Compose
- *(E2E 테스트)* Node.js 18+

### 로컬 개발

```bash
# 1. MariaDB 컨테이너 기동
docker compose -f docker-compose.local.yml up -d

# 2. 애플리케이션 실행
./gradlew bootRun
```

> http://localhost:8080

### 전체 스택

```bash
# .env 파일에 필수 환경변수 설정 후
docker compose up -d --build
```

### 관찰 가능성 스택 추가 실행

```bash
# Prometheus + Grafana 함께 실행 (observability 프로파일)
docker compose --profile observability up -d
```

> Grafana: http://localhost:3000 · Prometheus: http://localhost:9090

---

## 환경 변수

| 변수 | 필수 | 설명 |
|---|:---:|---|
| `KRAFT_DB_USER` | **항상** | 데이터베이스 사용자 |
| `KRAFT_DB_PASSWORD` | **항상** | 데이터베이스 비밀번호 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | 권장 | Ops API / Admin 접근용 토큰 (`X-Ops-Token` 또는 `Authorization: Bearer`) |
| `KRAFT_SECURITY_OPS_ALLOWED_IPS` | — | `/ops/**`, `/admin/ops/**` 허용 CIDR/IP 목록 *(기본: loopback + RFC1918 사설 대역)* |
| `KRAFT_SECURITY_ACTUATOR_ALLOWED_IPS` | — | `/actuator/**` 허용 CIDR/IP 목록 *(기본: loopback + RFC1918 사설 대역)* |
| `KRAFT_SECURITY_TRUSTED_PROXIES` | — | `X-Forwarded-For`를 신뢰할 프록시 CIDR/IP 목록 |
| `SPRING_PROFILES_ACTIVE` | 컨테이너 | `local` 또는 `prod` |
| `KRAFT_API_CLIENT` | — | `mock` / `smok` / `real` *(기본: `application.yml=mock`, `local=smok`, `prod=real`)* |
| `KRAFT_COLLECT_AUTO_ENABLED` | — | 자동 수집 스케줄러 *(기본: `true`)* |
| `KRAFT_TRACING_SAMPLE_RATE` | — | 트레이싱 샘플링 비율 *(기본: `0.0` / prod: `0.1`)* |
| `KRAFT_OTEL_ENDPOINT` | — | OTLP 엔드포인트 *(기본: `http://localhost:4318`)* |
| `KRAFT_GRAFANA_ADMIN_PASSWORD` | observability | Grafana 관리자 비밀번호 |

---

## 빌드 및 테스트

```bash
# 단위 + 통합 테스트
./gradlew test

# 전체 검사 (Checkstyle + SpotBugs + 테스트 + JaCoCo)
./gradlew check

# 엄격 정적 분석 + 커버리지 게이트 (CI와 동일)
./gradlew check -PstrictStatic=true -PstrictCoverage=true

# 성능 스모크 테스트
./gradlew performanceSmokeTest

# E2E 테스트 (Playwright)
docker run --rm -v "$PWD:/work" -w /work \
  mcr.microsoft.com/playwright:v1.60.0-noble \
  bash -lc "npm ci && npx playwright install chromium && npm run test:e2e"
```

### 커버리지 임계값

| 지표 | 최소 |
|---|---|
| LINE | 82% |
| BRANCH | 67% |
| METHOD | 88% |
| CLASS | 97% |

---

## 주요 엔드포인트

| Method | 경로 | 설명 |
|:---:|---|---|
| `GET` | `/` | 메인 페이지 (번호 추천 + 최신 당첨번호) |
| `GET` | `/frequency` | 번호 빈도 통계 |
| `GET` | `/rounds` | 회차 목록 |
| `GET` | `/fragments/recommend` | HTMX 번호 추천 프래그먼트 |
| `GET` | `/actuator/health` | 헬스체크 |
| `GET` | `/actuator/prometheus` | Prometheus 메트릭 *(prod 프로필 노출, IP allowlist 보호)* |
| `POST` | `/ops/collect` | 수동 당첨 번호 수집 트리거 *(Ops 전용)* |
| `GET` | `/ops/collect/status` | 수동/자동 수집 작업 상태 조회 *(Ops 전용)* |
| `POST` | `/ops/collect/missing` | 누락 회차만 1회 수집 *(Ops 전용)* |
| `GET` | `/ops/circuit-breakers` | 서킷브레이커 상태 조회 |
| `GET` | `/ops/recommend/stats` | 추천 생성 메트릭 스냅샷 |
| `POST` | `/ops/news/collect` | 뉴스 수동 수집 트리거 *(Ops 전용)* |

---

## 보안

| 항목 | 내용 |
|---|---|
| **Rate Limit** | IP × URI 당 분당 120 요청 (Fixed-window, Caffeine 기반) |
| **Ops 접근 제어** | `/ops/**`, `/admin/ops/**`에 대해 IP allowlist 적용 + 선택적 토큰 검증 (`X-Ops-Token`/Bearer) |
| **Actuator 접근 제어** | `/actuator/**`에 대해 IP allowlist 적용 *(Prometheus는 prod 프로필에서만 노출)* |
| **보안 헤더** | CSP · HSTS · X-Frame-Options · Referrer-Policy · Permissions-Policy |
| **감사 로그** | Ops 상태 변경 액션을 `kraft.audit` 로거로 구조화 기록 |

---

## 스케줄러

| 스케줄러 | 실행 시각 *(Asia/Seoul)* | 설명 |
|---|---|---|
| `WinningNumberAutoCollectScheduler` | **토 22:30 · 일 07:00** | 최신 회차 당첨 번호 수집 |
| `LottoFetchLogRetentionScheduler` | **매일 03:30** | 오래된 수집 로그 정리 *(기본 90일 보관)* |

---

## 라이선스

[LICENSE](LICENSE)

<div align="center">
<sub>kLo · portuna85 · 2026</sub>
</div>
