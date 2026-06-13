# KRAFT Lotto 재구축 설계서 v2

> 작성일: 2026-06-12
> 기반 문서: `rebuild_blueprint.md` (GPT 초안)
> 검증 기준: 현행 코드베이스 `kLo-main` 전수 분석 결과 (백엔드 198 + 테스트 103 Java 파일, Flyway V1–V25, CI/CD 5종 워크플로, 배포 스크립트 전체)
> 결론: **GPT 초안의 큰 방향(웹 중심, Flutter·뉴스 제거, backend/web 분리)은 채택한다. 다만 초안의 세부 설계 다수가 현행 코드베이스보다 후퇴하거나 사실관계가 틀려, 본 문서에서 재판단·재작성한다.**

---

## 0. GPT 초안 검토 결과 — 핵심 판정

GPT 초안은 현행 코드를 직접 보지 않고 작성된 문서다. 실제 코드베이스 분석 결과와 대조한 판정은 아래와 같다.

| # | GPT 초안 항목 | 판정 | 근거 (현행 코드 실측) |
|---|---|---|---|
| 1 | 뉴스 기능 "완전 제거" 작업 (체크리스트 20여 항목, 검증 SQL, 금지 라우트) | ❌ **대부분 기각** | 뉴스는 **이미 제거 완료** 상태다. V24가 테이블을 drop했고, `feature/news` 패키지·컨트롤러·스케줄러·프론트 라우트가 0건이다. 남은 것은 ① 구 마이그레이션 파일(새 baseline에서 자연 소멸) ② **`db-restore-drill.sh:64`가 아직 `news_articles`를 필수 검증** (현행의 실버그, §15.3) ③ 낡은 README 서술뿐이다. 제거 "프로젝트"가 아니라 **잔재 3건 정리**로 축소한다. |
| 2 | Flutter / FCM / 푸시 제거 | ✅ **채택** | `app/`(Dart 32파일), `feature/push`+`infra/fcm`(백엔드 7파일), `device_tokens` 테이블, `flutter-build.yml`이 실존. 제거 범위·체크리스트는 GPT안을 다듬어 유지한다 (§16). |
| 3 | web을 별도 컨테이너로 분리 | 🔧 **채택 + 근거 교체** | 현행은 Next.js **static export를 Spring jar에 내장**하는 구조인데, 이 구조가 실측된 SEO·캐싱 결함 5건의 직접 원인이다 (§3.1). 분리하되 단순 분리가 아니라 **standalone SSR/ISR 서버**로 전환한다 — 이것이 kraft.io.kr "색인 거의 안 됨" 문제와 `/latest` 동적 타이틀 P1을 구조적으로 해결한다. |
| 4 | DB 스키마 초안 (winning_numbers 축소, 신규 summary 스키마) | ❌ **기각 → 현행 스키마 승계** | GPT 스키마는 현행 V1 baseline의 **하위 호환 깨는 다운그레이드**다: `second_prize/second_winners/total_sales/first_accum_amount/raw_json` 누락 → 세후 수령액 계산기와 2등 보충(enrich) 로직이 깨진다. GPT가 제안한 CHECK 제약은 현행에 **이미 더 강하게 존재**한다 (정렬 제약 `n1<n2<…<n6`, 보너스 중복 금지 포함). 통계 summary 테이블도 GPT의 `from_round/to_round` 구조가 아니라 현행 구조(`stat_type/bucket_key`, ball 기반)를 그대로 가져와야 통계 코드가 무수정 이식된다 (§8). |
| 5 | saved_numbers를 `client_token_hash`(SHA-256)로 재설계 | ✅ **채택 + 보강** | 현행은 원문 `device_token`을 평문 저장한다 — GPT 제안이 실제 개선이다. 단 GPT가 누락한 것 보강: 기기당 100건 상한(현행 로직), 기존 운영 데이터 해시 이전 절차, 보존 정책, 중복 인덱스 제거 (§8.3). |
| 6 | Docker Compose / Caddy / 환경변수 샘플 | ❌ **기각 → 현행 포팅** | GPT 샘플은 현행 대비 전면 하향이다: 헬스체크·리소스 제한·`no-new-privileges`·`cap_drop`·`read_only`·digest 핀 부재, `ADMIN_PASSWORD` **평문** env(현행은 `{bcrypt}` 해시 + 5회 잠금 + 감사 로그), `mariadb:11` 부동 태그, `DB_HOST` 등 **환경변수 전면 개명**(현행 `KRAFT_*` 체계와 render-env.sh·검증기·드리프트 체커가 전부 무효화됨). 운영 계층은 현행 코드베이스에서 가장 검증된 부분 — **거의 그대로 이식**한다 (§10–§12). |
| 7 | API 계약 (recommend에 `strategy/include/exclude/score` 추가 등) | 🔧 **경로는 현행 동결, 신규 필드 기각** | kraft.io.kr은 **라이브 서비스**고 현행 API 경로는 GPT 추정과 거의 일치한다 (`POST /api/v1/numbers/recommend` 등 — §7에 실측 표). GPT가 끼워 넣은 `strategy: BALANCED`, `score: 82`, include/exclude는 존재하지 않는 기능의 스코프 크리프로, "도메인 기능 최소화" 원칙과 자기모순이다. 응답 body에 `requestId` 추가도 기각 — 현행은 `X-Request-Id` **헤더** 방식이며 모든 클라이언트·로그 상관관계가 이를 전제한다. |
| 8 | 메트릭 이름 신규 정의 (`kraft_lotto_fetch_success_total` 등) | ❌ **기각 → 현행 이름 유지** | 현행 `kraft_*` 메트릭 이름은 `alert_rules.yml`(8개 알림 룰)·Grafana 대시보드·smoke-test.sh의 메트릭 검증과 결합돼 있다. 이름을 바꾸면 모니터링 스택 전체를 재작성해야 한다. 이식 가치가 가장 높은 자산 중 하나 (§15). |
| 9 | sitemap에 `/saved`, `/status` 포함 | ❌ **기각** | `/saved`는 기기별 콘텐츠(크롤러에겐 빈 페이지), `/status`는 운영 페이지다. 색인 대상이 아니며 오히려 thin-content 신호가 된다. `noindex` 처리 (§14). |
| 10 | `check-no-removed-features.sh` 가드 스크립트 | 🔧 **채택 + 버그 수정** | GPT 스크립트는 **자기 자신과 `docs/removed-features.md`를 grep해 항상 실패**하는 자기참조 결함, `/news` 패턴의 과매칭 문제가 있다. 수정판 제공 (§17). |
| 11 | 새 Flyway baseline (뉴스 마이그레이션 미이식) | ✅ **채택** | 현행 V1조차 뉴스 테이블을 생성했다가 V24에서 drop하는 이력을 갖고 있어, 새 저장소에서 clean baseline 재작성이 맞다. 단 baseline 내용은 GPT 초안이 아니라 **현행 운영 스키마 스냅샷**이어야 한다 (§8.4). |
| 12 | 구현 순서 / 선별 이식 / 문서 구조(docs/) | ✅ **채택 + 구체화** | 방향은 맞다. 다만 "선별 이식" 범위가 과소평가됐다 — 백엔드 코어는 직전 감사에서 9.0/9.0을 받았고 테스트 103클래스가 있다. **재작성이 아니라 패키지 단위 이식**이 기본 전략이며, 클래스 단위 이식 맵을 §5.2에 명시한다. |

> **요약**: GPT 초안에서 살아남는 것 = 방향(범위 축소, 저장소 구조, web 분리, clean baseline, saved 해시화, 가드 스크립트 아이디어, 문서 체계).
> 본 문서가 교체하는 것 = 모든 세부 스키마·인프라·API·보안·메트릭 설계 (현행 검증 자산 승계 + 감사에서 발견된 결함 수정 내장).

---

## 1. 확정 방향

| 구분 | 결정 | GPT 초안 대비 |
|---|---|---|
| 프로젝트명 | **kraft-lotto (KRAFT Lotto)** 유지 | 동일 |
| 모바일 앱 / Flutter / FCM / 위젯 | 제거 | 동일 |
| 뉴스 기능 | (이미 제거됨) 잔재 3건 정리 + 새 baseline에서 미생성 | 작업량 재산정 |
| 웹 프론트엔드 | Next.js **standalone SSR/ISR** 컨테이너로 재구축 | static export → SSR 전환 명시 |
| 백엔드 | Spring Boot 4 / Java 25 — **현행 코드 패키지 단위 이식** | "재구축"이 아닌 "이식 중심" |
| DB | MariaDB 11.7 유지, **현행 스키마 승계** + saved_numbers 재설계 | 스키마 초안 교체 |
| 배포 | Docker Compose + Caddy — **현행 하드닝 수준 유지** | 샘플 교체 |
| 모니터링 | Prometheus/Grafana/Alertmanager — 현행 구성·메트릭명·알림룰 이식 (MVP부터 포함) | "선택 도입" 기각 |
| CI/CD | 현행 ci.yml/cd.yml 구조 승계 + 모노레포 경로 분리 | 신규 작성 기각 |
| 기존 운영 데이터 | **승계한다** (winning_numbers 1,100+ 회차 등) — 이전 절차 §8.5 | 초안에 누락됐던 결정 |

**기존 확정 결정(2026-06-08, improvement.md) 승계 매핑** — 재구축으로 효력이 바뀌는 항목을 명시한다:

| 기존 결정 | 새 프로젝트에서의 효력 |
|---|---|
| ① smok primary / public-data fallback | **유지.** `CompositeLottoApiClient` 체계 그대로 이식. |
| ② winning_stores V15 정규화 | **소멸** (테이블 자체가 V21에서 제거됨, 새 baseline에 미포함). |
| ③ Caddyfile `{$KRAFT_DOMAIN}` 수정 | **완료 상태로 이식** (현행 Caddyfile은 이미 올바름). |
| ④ Virtual threads 활성화 | **유지** (`spring.threads.virtual.enabled=true` 현행 적용 확인). |
| ⑤ Bootstrap PurgeCSS 단계 제거 | Phase 1 완료(232KB→51KB). 공개 페이지가 Tailwind로 전환되므로 Bootstrap은 **관리자 Thymeleaf 전용**으로 축소 — purge 파이프라인 그대로 이식, Phase 2–4는 관리자 화면 한정으로 재정의. |
| ⑥ /statistics 301 제거 | **소멸** (해당 경로 없음). |
| ⑦ H2 prod 번들 유지 (E2E용) | **유지.** Playwright가 jar를 H2 모드로 기동하는 현행 E2E 체계 이식. |
| WinningStore 인터페이스 골격 보존 | **폐기로 갱신 (요확인).** 클린 저장소에서 미사용 골격은 죽은 코드다. 판매점 기능 재도입 시 별도 설계로 신규 작성 — GPT 초안과 동일 결론이며, 기존 "골격 보존" 결정을 대체한다. |

---

## 2. 제품 범위

### 2.1 포함 기능 (GPT 초안 채택, 현행 기능과 1:1 대응 확인)

| 우선순위 | 기능 | 현행 구현 (이식 원천) |
|---|---|---|
| P0 | 최신/목록/상세 회차 조회 | `RoundsApiController`, `WinningNumberQueryService` |
| P0 | 번호 추천 + 규칙 조회 | `recommend` 패키지 전체 (규칙 7종, ConstraintAware 생성기) |
| P0 | 저장함 (브라우저 단위) | `saved` 패키지 — **토큰 해시화 재설계 (§8.3)** |
| P1 | 빈도/패턴/동반 통계 + 조합 분석 | `statistics` 패키지 + summary 캐시 3종 |
| P1 | 수집 (자동 스케줄 + Ops API + 관리자 수동) | `winningnumber` 패키지 (수집기·서킷브레이커·ShedLock) |
| P1 | 수집 상태/freshness/실패 로그 | `OpsMonitoringController`, `OpsFetchLogController` |
| P2 | 관리자 콘솔 (수집/캐시/감사/시스템/SEO) | `admin` 패키지 + Thymeleaf 템플릿 |
| P2 | 모니터링 (health/metrics/alerts) | actuator + Prometheus 스택 |

### 2.2 제외 기능

Flutter 앱, Android/iOS 빌드, FCM·푸시 토큰, 모바일 위젯, AdMob — GPT 초안과 동일.
뉴스 관련 항목은 "제외"가 아니라 **"새로 만들지 않음"**으로 표기한다 (이미 없음).

**잔존 모바일 트래픽 주의**: 기존 배포된 앱이 있다면 `/api/v1/push/**` 제거 후 404를 받는다. 공개 API의 나머지 경로·응답 형식은 동결(§7)하므로 설치된 앱의 조회 화면은 계속 동작한다. 앱 스토어 게시 이력이 있다면 별도 sunset 공지만 검토한다.

---

## 3. 아키텍처

### 3.1 핵심 결정 AD-1: Next.js static export 내장 → standalone SSR/ISR 분리

현행 "static export를 Spring jar에 내장" 구조에서 **실측으로 확인된 결함**과, SSR 전환 시의 해소 여부:

| # | 현행 실측 결함 | 심각도 | SSR/ISR 전환 시 |
|---|---|---|---|
| 1 | `/latest` 타이틀이 빌드 시점 고정 → 회차별 동적 타이틀 불가 (**kraft.io.kr P1 과제**) | P1 | ISR `revalidate` + 수집 이벤트 → on-demand revalidate 웹훅으로 **구조적 해결** (§6.3) |
| 2 | canonical / OG / Twitter / JSON-LD **전부 부재** — 구 Thymeleaf `base.html`에는 있었으나 Next 이전 시 누락된 회귀 | P0(SEO) | Next Metadata API(`metadataBase`, `alternates.canonical`, `openGraph`) + JSON-LD 컴포넌트로 복원 (§6.4) |
| 3 | sitemap이 존재하지 않는 root 경로 7건 광고 (`/methodology` 등 — 실제는 `/info/{slug}`, `/data-source`는 302) → soft-404/중복 신호 | P0(SEO) | sitemap을 Next `app/sitemap.ts`로 이전, 실경로만 등재 (§14) |
| 4 | `SpaFallbackController`가 미존재 경로를 `index.html` **200**으로 포워드 → 전면 soft-404 | P1(SEO) | SSR의 네이티브 `notFound()` → 정상 404 상태코드 |
| 5 | `spring.web.resources.cache.period=31536000`이 **HTML에도 적용** → 배포 후 재방문자에게 최대 1년 stale HTML (구 청크 404 → 페이지 파손) | P1 | Next 서버 기본 동작: HTML `no-cache`/`s-maxage`, `/_next/static` immutable — 캐시 계층 정상화 |
| 6 | static export의 인라인 RSC 스크립트 때문에 공개 경로 CSP가 `script-src 'unsafe-inline'` 강제 | P2(보안) | SSR에서 **nonce 기반 CSP** 적용 가능 — 관리자 경로와 동일 수준으로 상향 (§9.2) |
| 7 | trailing slash 이중 서빙 (`/latest`·`/latest/` 모두 200, canonical 부재) → 중복 콘텐츠 | P1(SEO) | Next `trailingSlash` 정책 단일화 + 308 리다이렉트 + canonical |

> 비용: Node 런타임 컨테이너 1기 추가 (standalone 기준 RSS ≈ 100–150MB). 현행 호스트 예산(app 1G + mariadb 512M + 모니터링 ≈ 640M) 대비 수용 가능. web 컨테이너 메모리 한도 256M로 책정.
>
> 대안(static export 유지 + Spring 측 7건 개별 패치)도 가능하나, #1·#6은 static export로는 근본 해결이 불가하므로 **SSR/ISR을 채택**한다.

### 3.2 전체 구성

```text
사용자 브라우저
    │
    ▼
Caddy ({$KRAFT_DOMAIN})
    │
    ├── /api/v1/*, /robots.txt(→web), /sitemap.xml(→web) 라우팅 규칙은 §10.2
    ├── /admin*, /ops*, /actuator*  → 공개 도메인에서 차단 (현행 정책 유지)
    ├── /api/v1/** ────────────────► backend:8080 (Spring Boot)
    └── 그 외 전체 ─────────────────► web:3000 (Next.js standalone)

Caddy ({$KRAFT_ADMIN_DOMAIN})
    └── /admin*, 정적 자원 ─────────► backend:8080 (Thymeleaf 관리자)

backend ──► mariadb / Scheduler+ShedLock / Caffeine / 외부 수집 클라이언트(smok→public-data)
web ──────► backend (SSR 데이터 페치, 내부망)
```

### 3.3 컨테이너 구성

```text
kraft-lotto
├── caddy        : TLS, reverse proxy        (64M,  digest 핀, 상시 기동 — §10.4)
├── web          : Next.js standalone        (256M, 신규)
├── backend      : Spring Boot API/Admin/Ops (1G,   현행 app 하드닝 그대로)
├── mariadb      : MariaDB 11.7              (512M)
├── prometheus   : 메트릭 (15d 보관)          (256M)
├── grafana      : 대시보드                   (256M)
├── alertmanager : Discord 알림               (64M)
└── node-exporter: 호스트 메트릭              (64M)
```

GPT 초안의 "모니터링 선택 도입"은 기각한다 — 현행 알림 룰(AppDown, CircuitBreakerOpen, FallbackExhausted, BackupNotRun 등 8종)과 백업 드릴 메트릭은 이미 운영 중인 안전망이며, 빼는 순간 회귀다.

---

## 4. 저장소 구조

GPT 초안 구조를 채택하되, 현행에서 검증된 보조 파일을 보강한다 (굵게 = GPT 초안에 없던 항목).

```text
kraft-lotto/
├── backend/
│   ├── build.gradle.kts / settings.gradle.kts / gradlew*
│   ├── **gradle.lockfile**            # 의존성 잠금 — Dockerfile에 반드시 COPY (§10.3)
│   ├── **config/checkstyle/**         # 정적 분석 설정 이식
│   ├── Dockerfile / **Dockerfile.ci**
│   └── src/main|test/...              # 패키지 구조 §5.1
├── web/
│   ├── package.json / next.config.mjs / tsconfig.json / tailwind.config.ts
│   ├── Dockerfile                     # multi-stage → standalone
│   └── src/app|components|lib/...
├── infra/
│   ├── docker-compose.yml / docker-compose.local.yml
│   ├── caddy/Caddyfile
│   ├── prometheus/ grafana/ alertmanager/   # 현행 디렉터리 통째 이식
│   └── **healthcheck.sh**
├── scripts/
│   ├── deploy/                        # render-env, validate-env, pull-and-up, rollback,
│   │                                  # wait-readiness, smoke-test, render-alertmanager … 현행 이식
│   ├── db-backup.sh / db-restore.sh / db-restore-drill.sh   # 드릴 테이블 수정 (§15.3)
│   ├── backup-timer/                  # systemd 타이머 이식
│   ├── **check_env_drift.py / check_utf8.py**   # CI 가드 이식
│   └── check-no-removed-features.sh   # §17 수정판
├── docs/                              # GPT 제안 채택: architecture/api/database/deployment/
│   │                                  # operations/removed-features (+ **seo.md** 추가)
├── tests/e2e/                         # Playwright (현행 smoke/admin/performance 이식)
├── .github/workflows/  ci.yml cd.yml pr.yml codeql.yml   (flutter-build.yml 미이식)
├── .github/dependabot.yml             # §12.4 보강판
├── .env.example / .env.prod.example / .env.local.example
└── README.md / LICENSE
```

**반드시 없어야 하는 경로**: `app/`, `flutter-build.yml`, `pubspec.*`, `feature/push`, `infra/fcm` — §17 가드로 CI 강제.

---

## 5. 백엔드 설계

### 5.1 패키지 구조

GPT 초안의 `common/winningnumber/recommend/statistics/saved/admin/ops/seo` 분류를 채택하되, 현행 패키지와의 매핑을 고정해 이식을 기계적으로 만든다.

```text
com.kraft.lotto
├── common/        ← 현행 support/* + infra/config/* (security, ratelimit, error, cache, logging 하위 분리)
├── winningnumber/ ← 현행 feature/winningnumber 그대로 (domain/application/infrastructure/web)
├── recommend/     ← 현행 feature/recommend 그대로
├── statistics/    ← 현행 feature/statistics 그대로
├── saved/         ← 현행 feature/saved + §8.3 재설계 반영
├── admin/         ← 현행 feature/admin + infra/config/Admin* 보안 구성
└── ops/           ← 현행 web/Ops* 컨트롤러·파사드
```

`seo/` 패키지는 **만들지 않는다** (GPT 초안 기각) — sitemap/robots는 web(Next)으로 이관한다 (§14). `SeoController`·`SeoPages`·`SpaFallbackController`는 미이식.

### 5.2 클래스 단위 이식 맵

> 원칙: **직전 감사 9.0/9.0을 받은 코드와 테스트 103클래스가 이 프로젝트의 최대 자산이다. 기본값은 "무수정 이식"이며, 수정은 아래 결함 목록에 한정한다.**

**[A] 무수정 이식 (패키지 이동만)**

| 영역 | 대상 |
|---|---|
| 수집 코어 | `LottoApiClient` 계열 전체 — `SmokLottoApiClient`, `PublicDataLottoApiClient`, `DhLotteryApiClient/Parser/Tracer`, `CompositeLottoApiClient`(2등 enrich 포함), `ApiCircuitBreaker(+Registry)`, `ApiRetrySupport`, `ApiCallExecutor`, `BackfillDelaySupport` |
| 수집 오케스트레이션 | `LottoCollectionCommandService`, `LottoSingleDrawCollector`, `LottoRangeCollector`, `CollectionRunState`, `CollectionEventNotifier`, `WinningNumberAutoCollectScheduler`(ShedLock+AtomicBoolean 이중 가드), `LottoFetchLog*`(보존 스케줄러 포함), `LocalHistoryInitRunner` |
| 영속화 | `WinningNumberPersister`, `WinningNumberUpsertExecutor`(REQUIRES_NEW native upsert), `WinningNumberEntity/Mapper/Repository`, `WinningNumberStatisticsRepository` |
| 추천 | `recommend` 패키지 전체 (규칙 7종, `ConstraintAwareLottoNumberGenerator`, 메트릭 기록기) |
| 통계 | `statistics` 패키지 전체 (`WinningStatisticsCacheService`의 summary→재계산 폴백 구조, `@EventListener` summary 갱신 포함) |
| 보안/공통 | `OpsAccessFilter`(constant-time 토큰 비교), `ActuatorAccessFilter`, `SecurityHeadersFilter`, `PublicRateLimitFilter`(sliding window+Caffeine), `WwwRedirectFilter`, `RequestIdFilter`(`X-Request-Id`), `ApiKeyFilter`, `LogSanitizer`, `IpRange`, `ApiResponse/ApiError/ErrorCode/BusinessException`, `GlobalExceptionHandler`, `PublicApiExceptionHandler`, `OpsExceptionHandler` |
| 설정/검증 | `Kraft*Properties` 일체(Fcm 제외), `RequiredConfigValidator`, `ProdConfigValidator`, `ProfilePolicyValidator`, `JdbcConnectivityValidator`, `DotenvEnvironmentPostProcessor`, `DatasourceUrlAutoFixer`, `FlywayConfig`(repair-on-start), `SchedulerLockConfig`, `AsyncConfig`(가상 스레드+MDC 전파), `ClockConfig`, `JacksonConfig`, `OpenApiConfig`, `CacheConfig` |
| 관리자 | `AdminSecurityConfig`(역할 4종, `{bcrypt}` 강제), `AdminLoginLockoutService/Filter`, `AdminAuthSuccess/FailureHandler`, `AdminAuditLog*`(보존 스케줄러 포함), 관리자 컨트롤러·Thymeleaf 템플릿 (뉴스 잔여 라벨만 정리) |
| 헬스 | `LottoApiHealthIndicator` |

**[B] 수정 이식 — 감사에서 확인된 결함을 이식 시점에 고정한다**

| # | 대상 | 결함 (실측) | 수정 |
|---|---|---|---|
| B-1 | `SavedNumbersService` | `ResponseStatusException`(400/422)을 던지지만 `PublicApiExceptionHandler`의 `@ExceptionHandler(Exception.class)`가 먼저 잡아 **500 + error 로그**로 응답됨 | `BusinessException` + `ErrorCode` 체계로 통일 (코드베이스 표준). 테스트 부재가 원인이었으므로 단위·슬라이스 테스트 신규 작성 |
| B-2 | `ClientIpResolver`, `IpAllowlist` | XFF의 각 후보를 `InetAddress.getByName()`으로 파싱 — 공격자가 헤더에 **호스트명**을 넣으면 요청당 블로킹 DNS 조회 발생 (DoS 벡터) | Java 25의 `InetAddress.ofLiteral()`로 교체 (리터럴 외 즉시 거부, DNS 미발생) |
| B-3 | `ApiCorsConfig` | `allowedMethods`에 **DELETE 누락**, `allowedHeaders`에 **X-Device-Token 누락** — 저장함 API 계약과 불일치 | DELETE + `X-Device-Token` 추가 |
| B-4 | `PublicRateLimitFilter` | (SSR 전환 영향) web 컨테이너의 서버측 페치가 단일 IP로 집계되어 사이트 전체가 한 버킷에 갇힘 | resolve된 IP가 trusted-proxy CIDR 내부이면 **rate-limit 우회**(내부 트래픽) + web은 SSR 페치 시 수신 요청의 클라이언트 IP를 `X-Forwarded-For`로 전달 (§9.3) |
| B-5 | `LottoApiClientConfig` | `public-data` 클라이언트만 서킷브레이커 미적용 (primary로 설정될 경우 무방비) + Javadoc `{@link SmokApiClient}` 오기 | 브레이커 일관 적용, Javadoc 수정 |
| B-6 | `SavedNumbersEntity` | `LocalDateTime.now()` 직접 호출 (코드베이스 표준은 `Clock` 주입) | `Clock` 주입으로 통일 |
| B-7 | `StatusApiController` | (선택) `appVersion/buildTime` 공개 노출 — 핑거프린팅 소지 | 유지 가능. 노출 범위 재확인만 |

**[C] 미이식 (폐기)**

`feature/push` + `infra/fcm` + `PushApiController` + `Platform`/`DeviceToken*` (Flutter 제거), `SeoController`/`SeoPages`/`SpaFallbackController` (web으로 이관), `WinningStore`/`WinningStoreApiClient` 골격 (§1 결정 갱신), `InfoPageController`의 `/data-source` 리다이렉트 (web에서 처리), 프론트 내장용 Gradle 태스크(`npmCi/buildFrontend/copyFrontend` — 분리로 불필요, **configuration-cache 재활성화 가능해짐**).

**[D] 신규 작성 (소량)**

`saved` 토큰 해시 처리(§8.3), 수집 이벤트 → web revalidate 웹훅 호출기(§6.3), `/api/v1/status`의 SSR 캐시 헤더 점검 정도.

### 5.3 백엔드 책임 경계

GPT 초안 §5.1과 동일하되 한 줄 추가: **백엔드는 더 이상 공개 HTML을 서빙하지 않는다** (관리자 Thymeleaf + 에러 페이지 제외). `spring.web.resources.*`의 1년 캐시 설정은 관리자 정적 자원(`/css /js /vendor /images`)에만 적용되도록 범위를 축소한다 — 현행 결함 #5의 백엔드 측 잔재 제거.

---

## 6. 웹(Next.js) 설계

### 6.1 렌더링 전략

| 페이지 | 전략 | revalidate |
|---|---|---|
| `/` `/latest` `/rounds` | **ISR** (회차 데이터 의존) | 300s + **on-demand** (§6.3) |
| `/frequency` `/stats` `/analysis` `/companion` | ISR | 1800s + on-demand |
| `/rounds/[round]` (상세 — 현행 쿼리 방식 유지 시 생략 가능) | ISR | 무기한 (과거 회차 불변) |
| `/info/[slug]` `/data-source` | **SSG** (정적) | — |
| `/saved` `/status` | 클라이언트 렌더 + **`robots: noindex`** | — |

### 6.2 라우트

GPT 초안 §6.2 채택 + 수정: `/data-source`는 현행처럼 `/info/data-source`로 통합하고 **루트 경로에서 301 리다이렉트** (next.config redirects). 사이트 전체 trailing slash 정책은 **비-슬래시 단일화**(`trailingSlash: false`, Next가 `/latest/`→`/latest` 308 처리) — sitemap·canonical과 형태 일치.

### 6.3 동적 메타데이터 — `/latest` P1 해결

```text
1. generateMetadata()에서 GET /api/v1/rounds/latest 페치
   → title: "제1175회 로또 당첨번호 (2026-06-06) | KRAFT Lotto"
2. ISR revalidate 300s를 기본 안전망으로 유지
3. 수집 파이프라인 연동(on-demand):
   backend WinningNumbersCollectedEvent(dataChanged=true)
     → POST http://web:3000/api/revalidate  (body: paths=[/,/latest,/rounds,...], 헤더: X-Revalidate-Secret)
     → web: revalidatePath() 실행
   - 시크릿: KRAFT_REVALIDATE_SECRET (render-env.sh 추가, 양쪽 컨테이너 주입)
   - 실패해도 수집 트랜잭션에 영향 없음 (비동기, 기존 @Async 리스너 패턴 재사용)
```

→ 추첨 직후 수 초 내 타이틀·OG·본문이 새 회차로 갱신된다. static export에서는 불가능했던 경로.

### 6.4 SEO 메타 복원 (회귀 수정)

구 Thymeleaf `base.html`에 존재했으나 Next 이전 시 누락된 요소를 전부 복원한다:

```text
- layout.tsx: metadataBase = new URL(KRAFT_PUBLIC_BASE_URL)
- 페이지별 alternates.canonical (전 페이지)
- openGraph: title/description/url/siteName/locale(ko_KR)/images(/images/og-kraft-lotto-1200x630.png — 기존 자산 이식)
- twitter: summary_large_image
- JSON-LD: WebSite + Organization (base.html 내용 이식) + /latest에 회차별 구조화 데이터
- 404: app/not-found.tsx (정상 404 상태코드)
```

### 6.5 저장함 토큰 정책

GPT 초안 §6.5 채택 (UUID 생성 → localStorage → `X-Device-Token` 헤더 → 서버는 SHA-256만 저장 → 로그 마스킹 `LogSanitizer` 연동). 헤더명 `X-Device-Token` 유지 — 현행 계약과 동일.

### 6.6 API 클라이언트 (`web/src/lib/api.ts`)

현행 `frontend/src/lib/api.ts`를 이식하되: 서버 컴포넌트용은 내부 URL(`KRAFT_BACKEND_INTERNAL_URL=http://backend:8080`), 브라우저용은 same-origin `/api/v1` — Caddy가 라우팅하므로 `NEXT_PUBLIC_API_BASE_URL`은 불필요 (GPT 초안 단순화).

---

## 7. API 계약 — 현행 동결

라이브 서비스이므로 공개 API는 **현행 경로·응답 형식 그대로** (실측 기준):

| Method | 경로 | 비고 |
|---|---|---|
| GET | `/api/v1/status` | appVersion/buildTime 포함, 5분 캐시 |
| GET | `/api/v1/rounds` `/rounds/latest` `/rounds/{round}` | latest 5m / history 1d 캐시 |
| POST | `/api/v1/numbers/recommend` | 현행 요청 형식 유지 — GPT의 strategy/include/exclude/score **추가 안 함** |
| GET | `/api/v1/numbers/recommend/rules` | 1h 캐시 |
| GET | `/api/v1/stats/frequency` `/patterns` `/companion` | 10m public 캐시 |
| POST | `/api/v1/stats/analysis` | |
| GET/POST/DELETE | `/api/v1/saved` `/saved/{id}` | `X-Device-Token` 필수, 오류 응답을 ApiResponse 표준으로 정정 (B-1) |
| — | `/api/v1/push/**` | **제거** (404) |

Ops API: `/ops/collect{,/missing,/refresh,/status}`, `/ops/circuit-breakers`, `/ops/data-freshness`, `/ops/recommend/stats`, `/ops/fetch-logs/{failures,failure-reasons,failure-overview,retention-status}` — 현행 동결 (`/ops/news/collect`는 현행에도 없음).
관리자: 현행 경로 동결 (뉴스 메뉴는 현행에도 없음).
공통 응답: 현행 `ApiResponse{success,data,error}` + **`X-Request-Id` 헤더** — GPT의 body `requestId` 기각.

---

## 8. 데이터베이스 설계

### 8.1 원칙

```text
1. 새 Flyway 히스토리: V1 단일 clean baseline (GPT 채택)
2. baseline 내용 = 현행 운영 스키마(V25 시점) 스냅샷 − 제거 테이블 + saved_numbers v2 (GPT 스키마 기각)
3. 벤더 분기(db/vendor/{h2,mariadb,mysql}) 체계 유지 — H2 E2E(결정 ⑦)와 CI flyway migration-validate가 의존
4. 미생성: news_* 3종, winning_stores, device_tokens
```

### 8.2 테이블 목록

| 테이블 | 출처 | 비고 |
|---|---|---|
| `winning_numbers` | 현행 그대로 | CHECK 제약 17종(범위·정렬·보너스중복·음수금지) **이미 보유** — GPT 제안보다 강함. `second_prize/second_winners/total_sales/first_accum_amount/raw_json/version` 유지 (세후 계산기·enrich·UNCHANGED 판정 의존) |
| `lotto_fetch_logs` | 현행 그대로 | `api_client/retry_count/latency_ms` 진단 컬럼(V16) 포함 |
| `winning_number_frequency_summary` | 현행 그대로 | GPT의 "필요 시 대체" 기각 — `WinningStatisticsCacheService`가 직접 의존 |
| `pattern_stats_summary` | 현행 그대로 | 현행 `stat_type/bucket_key` 구조 (GPT의 from/to_round 구조 기각) |
| `companion_pair_summary` | 현행 그대로 | ball 기반 현행 구조 |
| `admin_audit_log` (+V19 복합 인덱스) | 현행 그대로 | 보존 스케줄러 의존 |
| `saved_numbers` | **v2 재설계** | §8.3 |
| `shedlock` | 현행 그대로 | |

### 8.3 saved_numbers v2 (GPT 채택 + 보강)

```sql
CREATE TABLE saved_numbers (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    client_token_hash CHAR(64)     NOT NULL,           -- SHA-256(hex) of X-Device-Token
    numbers           VARCHAR(32)  NOT NULL,            -- "1,7,13,22,34,41" 정렬 저장 (현행 인코딩 유지)
    label             VARCHAR(100) NULL,
    source            VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    created_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saved_client_numbers (client_token_hash, numbers),   -- 중복 저장 방지 (GPT 채택)
    INDEX idx_saved_client_created (client_token_hash, created_at DESC)
);
```

보강 사항: 기기당 **100건 상한 유지**(현행 로직 이식), 현행의 중복 인덱스(`idx_saved_numbers_token` — 복합 인덱스의 좌측 prefix) 미이식, 비활성 데이터 보존 정책(예: created_at 2년 경과 시 정리 — 기존 retention 스케줄러 패턴 재사용, P2), UNIQUE 충돌은 409가 아닌 "이미 저장됨" 멱등 응답.

### 8.4 Flyway 구성

```text
backend/src/main/resources/db/
├── migration/V1__baseline_clean.sql        # §8.2 전체 (saved_numbers 제외)
├── migration/V2__add_saved_numbers.sql     # v2 스키마
└── vendor/{h2,mariadb,mysql}/              # 방언 차이 발생 시에만 사용 (구조 유지)
```

뉴스·푸시·판매점 관련 마이그레이션 이력은 **이식하지 않는다** (GPT §9.2 채택 — 단 사유는 "기능 제거"가 아니라 "이미 없는 테이블의 생성/삭제 이력 불요").

### 8.5 운영 데이터 이전 (GPT 초안 공백 보완)

기존 DB는 건강하며 회차 데이터는 재수집 가능하지만 시간이 들므로 **dump→load 이전**을 표준 절차로 한다:

```text
1. 현행 db-backup.sh로 전체 백업 (안전망)
2. 이전 대상 데이터 덤프: winning_numbers, admin_audit_log
   (lotto_fetch_logs는 선택 — 90일 보존 데이터라 포기 가능 / summary 3종은 재계산되므로 제외)
3. 새 스키마 DB 생성 → Flyway V1·V2 적용 → 데이터 import
4. saved_numbers 변환 (사용자 저장 데이터 보존):
   INSERT INTO saved_numbers (client_token_hash, numbers, label, source, created_at)
   SELECT SHA2(device_token, 256), numbers, label, 'MANUAL', saved_at FROM old.saved_numbers;
   → 클라이언트는 기존 토큰을 계속 전송, 서버가 해시 매칭 → 사용자 무중단
5. 검증: 회차 수 일치, /api/v1/rounds/latest 정합, 저장함 조회 스모크
6. 컷오버: 신규 compose 기동 → smoke-test.sh → DNS/Caddy 유지 (도메인 동일)
```

---

## 9. 보안 설계

### 9.1 기본 원칙 — 현행 보안 계층 전체 이식

GPT 초안 §10의 표를 다음으로 대체한다 (현행 실구현 기준):

| 영역 | 정책 (이식 원천) |
|---|---|
| 공개 API | per-IP sliding-window rate limit (120/min, Caffeine 키 상한 10k) + `X-RateLimit-*` 헤더 |
| 클라이언트 IP | XFF **우→좌 순회**로 신뢰 프록시 제거 (위조 삽입 차단) — B-2 수정 포함 |
| Ops API | IP allowlist + 토큰 이중 인증, **constant-time 비교** (`X-Ops-Token`/`Bearer`) |
| 관리자 | Spring Security 폼 로그인 + **`{bcrypt}` 해시 강제** + username+IP 기준 5회/15분 잠금 + 감사 로그 + 역할 4종 — GPT의 `ADMIN_PASSWORD` 평문 env **기각** |
| Actuator | `health,info,prometheus`만 노출 + IP allowlist 필터 + 공개 도메인 Caddy 차단 |
| 헤더 | CSP·HSTS·XFO·Referrer·Permissions-Policy (SecurityHeadersFilter 이식) |
| 로그 | `LogSanitizer` (토큰·민감 경로 마스킹), `X-Request-Id` MDC 전파 |
| 시크릿 주입 | render-env.sh의 단일따옴표 인코딩 + 개행 거부 + umask 077 + compose config 검증 |

### 9.2 CSP 상향 (SSR 전환의 부수 이득)

현행: 공개 경로 `script-src 'unsafe-inline'` 강제(static export 인라인 스크립트), 관리자만 nonce.
신규: web이 요청별 nonce를 생성해 Next에 주입(middleware) → **공개 경로도 nonce 기반 CSP**. AdSense 활성 시 지시문 추가 로직(현행 `appendAdsenseCsp`)은 web 측으로 이관. 관리자 도메인 CSP는 백엔드 현행 유지.

### 9.3 내부 트래픽과 rate limit (신규 고려사항)

```text
- compose 네트워크에 명시적 ipam subnet 고정: 172.28.0.0/16 (예약대역 충돌 회피)
- KRAFT_SECURITY_TRUSTED_PROXIES 기본값을 동일 CIDR로 정렬
  (현행 결함: 기본 172.18.0.0/16인데 docker가 다른 서브넷 배정 시
   전 트래픽이 caddy IP 한 버킷으로 묶여 사이트 전체 429 위험)
- web→backend SSR 페치: 요청 컨텍스트가 있으면 클라이언트 IP를 XFF로 전달,
  ISR 백그라운드 재생성처럼 컨텍스트가 없으면 내부 CIDR 우회 규칙(B-4) 적용
```

### 9.4 저장함 보안

GPT §10.3 채택 (긴 랜덤 토큰, 해시만 저장, query string 금지, 헤더 전용, 로그 원문 금지) + 기기당 상한·rate limit으로 토큰 난사 비용 제한.

---

## 10. 배포 / 인프라

### 10.1 docker-compose (현행 이식 + web 추가)

현행 compose의 하드닝을 **서비스별로 그대로** 가져간다: 전 서비스 `no-new-privileges`, backend `cap_drop: ALL` + `read_only` + tmpfs + `pids: 256`, 리소스 한도, json-file 로그 로테이션, healthcheck, caddy digest 핀. 변경점만 기술:

```yaml
# 변경/추가분 개요 (전체는 현행 docker-compose.yml 기반)
services:
  web:
    image: ${KRAFT_WEB_IMAGE_REF}:${KRAFT_WEB_IMAGE_TAG}   # GHCR, CI 빌드
    user: "10001:10001"
    read_only: true
    tmpfs: ["/tmp:size=32m"]                                # Next 캐시 디렉터리는 named volume
    security_opt: ["no-new-privileges:true"]
    cap_drop: ["ALL"]
    environment:
      KRAFT_BACKEND_INTERNAL_URL: http://backend:8080
      KRAFT_PUBLIC_BASE_URL: ${KRAFT_PUBLIC_BASE_URL}
      KRAFT_REVALIDATE_SECRET: ${KRAFT_REVALIDATE_SECRET}
    healthcheck: { test: ["CMD", "node", "healthcheck.js"], interval: 15s, ... }
    deploy: { resources: { limits: { memory: 256m, cpus: "0.50" } } }
  caddy:
    # 변경: profiles: ["tls"] 제거 → 상시 기동 (§10.4)
networks:
  kraft-net:
    ipam: { config: [ { subnet: 172.28.0.0/16 } ] }         # §9.3
```

이미지 핀 정책: caddy처럼 **mariadb/prometheus/grafana/alertmanager/node-exporter도 digest 핀**으로 통일 (현행은 caddy만 핀 — 일관성 보완, Dependabot docker 업데이트가 digest 갱신 PR 생성).

### 10.2 Caddy 라우팅 (현행 Caddyfile 기반 수정)

```caddyfile
{$KRAFT_DOMAIN} {
    header Strict-Transport-Security "max-age=31536000; includeSubDomains"
    encode zstd gzip                       # GPT 제안 zstd 채택 (소폭 개선)

    @blocked path /actuator* /ops* /admin*
    respond @blocked 403                   # 현행 정책 유지

    handle /api/v1/* {
        reverse_proxy backend:8080 { header_up X-Real-IP {remote_host} ... }
    }
    handle {                               # 페이지·sitemap.xml·robots.txt → web
        reverse_proxy web:3000 { header_up X-Real-IP {remote_host} ... }
    }
}

{$KRAFT_ADMIN_DOMAIN} {
    # 현행 블록 그대로 (rawBlocked, notAdmin, X-Admin-Entry) — backend:8080
}
```

CI의 `caddy validate` 단계(현행) 유지.

### 10.3 Dockerfile

**backend/Dockerfile** — 현행 이식 + 결함 2건 수정:
```text
1. COPY gradle.lockfile ./    ← 현행 누락: 잠금 미적용 빌드로 CI와 의존성 불일치 가능 (재현성 결함)
2. 프론트 내장 단계 전부 제거  ← 분리로 해소. 현행 Dockerfile은 frontend/를 COPY하지 않아
   로컬 docker build 산출물이 공개 페이지 없는 jar였던 결함도 함께 소멸
3. 그 외 동일: temurin digest 핀, 비루트 uid 10001, healthcheck.sh, JAVA_OPTS(G1, MaxRAMPercentage=75)
```
**web/Dockerfile** — multi-stage: `node:22-slim(digest)` build → `output: 'standalone'` 복사 → 비루트 → `HEALTHCHECK`.
**Dockerfile.ci** 패턴(빌드 산출물 COPY만) 양쪽에 유지 — CI가 러너에서 빌드 후 패키징.

### 10.4 운영 footgun 제거 — caddy profile

현행 `caddy: profiles: ["tls"]`는 `COMPOSE_PROFILES` 미설정 상태의 `docker compose down/up --remove-orphans`가 **caddy를 orphan으로 간주해 제거(TLS 전면 중단)** 하는 함정이다. 신규에서는 profile을 없애고 상시 서비스로 둔다. 로컬은 `docker-compose.local.yml`(caddy 없이 web:3000·backend:8080 직접 노출)로 분리.

### 10.5 배포 파이프라인

현행 cd.yml 흐름 유지, 이미지가 2개가 된 점만 반영:
```text
render-env(.env) → render-alertmanager → compose config 검증
→ GHCR pull (backend, web 두 이미지, 태그=SHA)
→ patch-flyway-history(필요 시) → pull-and-up.sh: up -d --no-deps backend web
→ prod 프로필 검증 → wait-readiness(backend) + web 헬스 → 실패 시 rollback.sh(두 이미지 모두 previous 태그 관리)
→ smoke-test.sh
```
smoke-test 수정: `/data-source` 302 기대값 → **301**(next.config redirects 기준)로 갱신, `/latest` 응답에 회차 문자열 포함 검증 추가(동적 타이틀 회귀 가드), web 컨테이너 헬스 추가.

---

## 11. 환경 변수

**원칙: 현행 `KRAFT_*` 네이밍 전면 유지** — `render-env.sh`·`validate-env.sh`·`RequiredConfigValidator`·`check_env_drift.py`가 키 이름에 결합돼 있다. GPT의 `DB_HOST/OPS_API_KEY/ADMIN_PASSWORD` 개명은 기각.

변경분만:
```env
# 추가
KRAFT_WEB_IMAGE_REF= / KRAFT_WEB_IMAGE_TAG=
KRAFT_BACKEND_INTERNAL_URL=http://backend:8080
KRAFT_REVALIDATE_SECRET=
# 제거 (Flutter/FCM)
KRAFT_FCM_ENABLED / KRAFT_FCM_CREDENTIALS_PATH / KRAFT_FCM_DRAW_RESULT_TOPIC / KRAFT_FCM_STALE_CLEANUP_CRON
```
`check_env_drift.py`를 compose의 web 서비스 키까지 검사하도록 1줄 확장. `.env.example` 3종 체계(공통/local/prod) 유지.

---

## 12. CI/CD

### 12.1 ci.yml — 현행 구조 승계 + 모노레포 분리

```text
backend-build-test : UTF-8 검사, env drift, gradlew test bootJar -PstrictCoverage=true,
                     jar 내 application*.yml 3종 검증 (현행 단계 전부 유지)
web-build-test     : npm ci → lint → typecheck(tsc --noEmit) → vitest → next build
                     ← 현행 공백 보완: PR에서 프론트 타입 오류가 main까지 통과되던 문제
static-analysis    : SpotBugs + Checkstyle -PstrictStatic=true (병렬, 현행)
caddy-validate     : 현행 유지
docker-publish     : backend·web 두 이미지 GHCR push(SHA) + provenance attestation (현행 패턴 ×2)
e2e-smoke          : Playwright — jar(H2 모드) + next standalone 동시 기동 스크립트로 갱신
security-scan      : Trivy(HIGH/CRITICAL, exit 1) + Syft SBOM — 두 이미지 모두 (현행 패턴 ×2)
migration-validate : Flyway CLI vs 실 MariaDB 11.7 (현행 유지, locations만 새 경로)
removed-feature-guard : §17 스크립트
promote            : 전 게이트 통과 시 SHA→latest ×2
lighthouse / performance-smoke : 주간 스케줄 (현행 유지)
```

### 12.2 pr.yml
현행 + `web-build-test` 추가 (위 공백 보완).

### 12.3 codeql.yml
현행 유지 (java) + `javascript-typescript` 언어 추가.

### 12.4 dependabot — 현행 공백 보완

```yaml
- gradle  /backend          # 그룹핑 현행 유지
- npm     /web              # ← 현행 누락: Next.js 보안 업데이트 미관리 상태였음
- npm     /                 # e2e 도구 (현행)
- docker  /backend, /web, /infra
- github-actions /
# pub(Flutter) 미등록 — GPT 동일
```

---

## 13. 테스트

| 계층 | 내용 |
|---|---|
| 백엔드 단위/슬라이스/통합 | **현행 테스트 103클래스를 코드와 함께 이식** — 이것이 재구축 리스크를 통제하는 핵심. Testcontainers(MariaDB) 통합·Flyway 검증·property 테스트 포함 |
| 백엔드 신규 | `saved`(B-1 회귀 포함)·revalidate 웹훅·rate-limit 내부우회(B-4) — 현행 커버리지 공백이던 영역 |
| 커버리지 게이트 | 현행 임계 유지: Line 82 / Branch 65 / Method 88 / Class 97 (strictCoverage) |
| web | typecheck + lint + vitest(컴포넌트·api 래퍼) + Playwright E2E(홈/최신/추천/저장·삭제/404 상태코드/canonical 존재) |
| 제거 회귀 | `POST /api/v1/push/token → 404`, `GET /news → 404` 를 smoke에 포함 (GPT §15.3 축약 채택) |
| SEO 회귀 (신규) | E2E에서 `sitemap.xml`의 모든 URL fetch → **전부 200 + canonical 자기참조** 검증 — 현행 결함 #3·#7의 재발 방지 게이트 |

---

## 14. SEO 설계 — kraft.io.kr 색인 워크스트림 직결

### 14.1 sitemap (web `app/sitemap.ts`, 동적 lastmod = 최신 추첨일 API 페치)

```text
포함: /  /latest  /rounds  /frequency  /stats  /analysis  /companion
      /info/data-source  /info/methodology  /info/faq  /info/privacy
      /info/terms  /info/contact  /info/responsible-play
제외: /saved /status (noindex 메타 부여)  /admin /ops /api /actuator
```
→ 현행 결함 #3(미존재 root 경로 7건) 해소: **실존 URL만, 리다이렉트 없는 최종 형태로** 등재.

### 14.2 robots (web `app/robots.ts`)
현행 정책 이식: `Disallow: /admin /ops /actuator /api/` + Sitemap 절대 URL.

### 14.3 재색인 작업 연동
재구축 배포 직후: GSC sitemap 재제출 → 핵심 4페이지(/, /latest, /rounds, /frequency) 색인 요청 → Naver Search Advisor 동일 절차. canonical·OG·JSON-LD·동적 타이틀·정상 404가 갖춰진 상태에서의 재요청이므로, 기존 "거의 미색인" 상태의 원인 요소가 모두 제거된 채 재평가받는다. 검증은 §13의 SEO 회귀 E2E가 상시 수행.

---

## 15. 운영

### 15.1 수집 스케줄 — 현행 유지
토 22:30 + 일 07:00 (KST, ShedLock `collect-all`), 실패 시 fallback 체이닝·서킷브레이커·Discord 알림. GPT의 "토 21:00 이후" 단순화 기각 — 현행 이중 크론이 추첨 지연·재수집을 이미 커버.

### 15.2 메트릭 / 알림 — 이름 동결
`kraft_api_circuit_breaker_state`, `kraft_collect_auto_*`, `kraft_api_fallback_*`, `kraft_backup_*` 등 **현행 이름 그대로** → `alert_rules.yml` 8종·Grafana 대시보드·smoke 메트릭 검증 무수정 이식. 추가 1건: `kraft_web_revalidate_total{result}` (신규 웹훅 관측).

### 15.3 백업 / 복원 드릴

```text
- db-backup.sh 이식 (defaults-extra-file 방식 — CLI 비밀번호 노출 없음, GPG 옵션, rclone 오프사이트)
- 보존: GPT의 GFS(일7/주4/월6) 채택 — 현행(일7/원격30)보다 개선
- db-restore-drill.sh 필수 테이블 수정:
    현행: (winning_numbers news_articles admin_audit_log flyway_schema_history)
                          ^^^^^^^^^^^^^ V24에서 drop된 테이블 — 현행의 실버그(드릴 항상 실패)
    신규: (winning_numbers saved_numbers admin_audit_log flyway_schema_history)
- systemd 타이머(backup-timer/) + kraft_backup_restore_drill_overdue 메트릭 체계 이식
```

---

## 16. 제거 체크리스트 (재산정판)

### 16.1 Flutter / 푸시 (실작업 — GPT 채택·압축)
```text
[ ] app/ 미이식 (1.1MB, Dart 32파일)            [ ] flutter-build.yml 미이식
[ ] feature/push + infra/fcm 미이식 (7파일)      [ ] PushApiController·/api/v1/push/** 미이식
[ ] device_tokens 테이블 baseline 미포함          [ ] KRAFT_FCM_* env 4종 제거
[ ] dependabot pub 미등록                        [ ] README·docs에서 모바일 서술 제외
[ ] firebase-admin·grpc-netty-shaded 의존성 제거 (CVE 오버라이드 1건도 함께 소멸)
```

### 16.2 뉴스 (잔재 정리 — 사실관계 정정판)
```text
[ ] db-restore-drill.sh 필수 테이블에서 news_articles 제거 (§15.3 — 유일한 코드 수정)
[ ] 뉴스 마이그레이션 이력 미이식 (V3/V6/V8/V10/V14/V17/V18/V20/V24 — baseline 재작성으로 자동)
[ ] README 재작성 시 뉴스 서술 제외 (현행 README가 stale)
[ ] admin/audit.html의 뉴스 action 라벨 잔재 정리
※ 컨트롤러·서비스·스케줄러·라우트·테이블 삭제 항목은 해당 없음 — 이미 존재하지 않음
```

### 16.3 문서
GPT §18.4 채택 + `docs/seo.md` 추가. `improvement.md`(기존 감사 문서)는 `docs/archive/`로 보존 — 본 문서 §5.2 [B]가 그 후속 결함 목록을 승계한다.

---

## 17. 제거 가드 스크립트 v2 (자기참조 결함 수정)

```bash
#!/usr/bin/env bash
# scripts/check-no-removed-features.sh
set -euo pipefail
FAIL=0

# 1) Flutter 부재
[ -d "app" ] && { echo "ERROR: app/ must not exist"; FAIL=1; }
find . -path ./.git -prune -o \( -name 'pubspec.yaml' -o -name '*.dart' \) -print | grep -q . \
  && { echo "ERROR: Flutter files remain"; FAIL=1; }

# 2) 푸시/FCM 부재 — 소스 한정, 자기 자신·문서 제외 (GPT 초안: repo 전체 grep → 스크립트 자신과
#    docs/removed-features.md에 항상 매칭되어 영구 실패하던 결함 수정)
SRC_DIRS=(backend/src web/src infra scripts/deploy)
if grep -RIn --exclude="$(basename "$0")" -E 'feature/push|infra/fcm|firebase-admin|device_tokens|/api/v1/push' \
     "${SRC_DIRS[@]}" 2>/dev/null; then
  echo "ERROR: push/FCM references remain in source"; FAIL=1
fi

# 3) 뉴스 부재 — 단어 경계로 과매칭 방지 (구 초안의 '/news' 패턴은 newsletter 등 오탐)
if grep -RInw --exclude="$(basename "$0")" -E 'news_articles|news_blocked_domain|news_blocked_keyword' \
     "${SRC_DIRS[@]}" 2>/dev/null; then
  echo "ERROR: news schema references remain"; FAIL=1
fi

exit "$FAIL"
```

---

## 18. 구현 순서 (port-first 전략)

```text
Phase 0  보존·준비 (0.5d)
  기존 repo 태깅·백업 / improvement.md → docs/archive / 새 repo 생성 / 본 문서 → docs/architecture.md 분배
Phase 1  골격 (1d)
  모노레포 구조 / backend Gradle(lockfile 포함) / web Next 스캐폴드 / ci.yml·pr.yml 기본 / 가드 스크립트
Phase 2  백엔드 이식 (2–3d)
  V1·V2 baseline → [A] 패키지 일괄 이식(테스트 동반) → [B] 결함 6건 수정 → [C] 미이식 확인
  → strictCoverage·strictStatic 통과가 Phase 완료 조건
Phase 3  web 구축 (3–4d)
  레이아웃·메타 복원(§6.4) → 페이지 이식(현행 frontend/ 컴포넌트 재사용) → ISR·revalidate 웹훅
  → sitemap/robots → saved 토큰 해시 연동 → E2E·SEO 회귀
Phase 4  인프라·배포 (1–2d)
  compose(+web, ipam, profile 제거) / Caddyfile / Dockerfile×2 / cd.yml / smoke-test 갱신
Phase 5  컷오버·안정화 (1d + 관찰)
  §8.5 데이터 이전 → 배포 → GSC/Naver 재제출(§14.3) → 드릴 1회 실행으로 §15.3 수정 검증
  → 알림·대시보드 정상 확인 → 구 저장소 archive
```

---

## 19. 완료 기준

**기능**: GPT §20.1 채택 (12항목) + 추가 — `/latest` 타이틀에 최신 회차 번호 포함, 미존재 경로 404 상태코드, 저장함 기존 사용자 데이터 조회 가능(이전 검증).
**제거**: §16 체크리스트 전항 + 가드 스크립트 CI 통과.
**품질 게이트** (현행 9.0/9.0 기준 승계): `strictCoverage`(82/65/88/97) PASS, `strictStatic` PASS, Trivy HIGH/CRITICAL 0(두 이미지), migration-validate PASS, SEO 회귀 E2E PASS, smoke-test PASS, 복원 드릴 1회 성공.

---

## 20. 한 줄 결론

**GPT 초안의 방향(웹 중심·Flutter/뉴스 제외·backend/web 분리)은 채택하되, 실행 설계는 "재작성"이 아니라 "검증된 현행 자산의 이식 + 감사 결함 13건의 동시 수정"으로 정의한다 — 특히 web은 static export가 아닌 SSR/ISR로 세워 kraft.io.kr 색인 문제의 구조적 원인을 제거한다.**
