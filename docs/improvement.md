# kraft-lotto (kLo) 개선 분석서 — 정본 (전체 약점 포함 · 제약 없음)

> 대상: `portuna85/kLo` (main 스냅샷, 2026-06-02) · Java 25 · Spring Boot 4.0.6 · Gradle 9.5.1 · MariaDB 11.7 · Thymeleaf+HTMX · Docker
> 본 문서는 **AGENTS.md의 수정 금지/허용 범위를 적용하지 않는다.** 모든 영역(앱·프론트·CI/CD·인프라·운영)을 동등한 개선 대상으로 본다.
> 코드에서 확인된 약점과 **운영·인프라 약점까지 전부** 백로그에 편입한다.

## 0. 현재 성숙도와 이 문서의 목적

| 축 | 점수 | 한 줄 |
| --- | --- | --- |
| 완성도(코드·기능·테스트) | **8.5 / 10** | 거의 상용급 |
| 운영 성숙도(operational readiness) | **6.0 / 10** | 단일점·무알림·수동백업이 발목 |
| 종합 | **7.0 / 10** | "포트폴리오 9점, 24/7 서비스 6점" |

핵심 진단: **코드 완성도가 운영 하드닝을 앞질러 있다.** 이 백로그는 그 격차를 메우는 것을 목표로 하며, 따라서 **운영·인프라 항목(O 시리즈)을 최상위로** 둔다.

**우선순위 범례** — `P0` 즉시 / `P1` 단기 / `P2` 중기 / `P3` 선택
**근거 표기** — (confirmed) 코드/구성에서 직접 확인 · (infra) 저장소 밖 운영 사실로 추정(다르면 알려주면 조정)

**임팩트 Top 10 (운영 우선)**

| 순위 | ID | 항목 | 유형 |
| --- | --- | --- | --- |
| 1 | O-2 | 능동 알림 부재 → Alertmanager/통지 | 운영 |
| 2 | B-1 | 외부 수집 소스 자동 페일오버 | 백엔드 |
| 3 | O-4 | 백업 자동화·오프사이트·복구 드릴 | 운영 |
| 4 | O-1 | 단일 호스트 SPOF → DB/러너 분리 | 인프라 |
| 5 | O-5 | 리버스 프록시/TLS를 코드로(IaC) | 인프라 |
| 6 | O-3 | 관측 스택 상시 구동 | 운영 |
| 7 | C-1 | build-once(CI 푸시 → CD digest pull) | CI/CD |
| 8 | F-1 | Bootstrap CSS 슬리밍 + webjar 제거 | 프론트 |
| 9 | B-2 | H2 prod 런타임 제거 | 백엔드 |
| 10 | B-3 | `lotto_fetch_logs` 중복 인덱스 정리 | 백엔드 |

---

## 1. 운영·인프라 (Operations & Infrastructure) — 가장 큰 약점 군

### O-1. 단일 호스트 SPOF `P1` (infra)
**현황** — `docker-compose.yml` 단일 노드에 app + MariaDB(+ observability 프로파일 + CD self-hosted 러너)가 동거(정황). ShedLock·`@SchedulerLock`은 멀티노드 준비가 돼 있으나 실제 토폴로지는 1대.
**리스크** — 호스트 1대 장애 = 서비스·DB·배포가 동시 다운. 단일 박스에서 자원 경합(앱 1g/DB 512m 제한).
**조치** — (단기) **DB를 분리**(별도 호스트/관리형 MariaDB 또는 최소한 별도 볼륨·백업 경로). 배포 러너 분리(O-6). (중기) 앱 2인스턴스 + 앞단 프록시 LB — ShedLock이 스케줄 중복을 막으므로 **수평 확장이 안전**. (취미 규모 현실해: "DB 분리 + 백업 오프사이트(O-4)"만으로도 체감 개선 큼.)

### O-2. 능동 알림 부재 `P0` (운영)
**현황(confirmed)** — `docker/prometheus/prometheus.yml`에 **alerting rule도 Alertmanager도 없다**(scrape만). Grafana는 있으나 통지 경로 없음. 반면 코드엔 `[ALERT]` 로그 + 풍부한 메트릭(`kraft.collect.auto.error`, `kraft.collect.auto.round.failure`, `kraft.api.circuit_breaker.transitions/state`, B-1의 `kraft.api.fallback.*`)이 이미 존재.
**리스크** — 수집 실패·회로 open·readiness down을 **사람이 봐야 안다.** 주말 자동 수집(토 22:30 / 일 07:00 / 월 10:10)이 조용히 실패하면 회차 누락이 며칠 방치된다 — 이 서비스의 본질 기능 손상.
**조치** — Alertmanager(또는 Grafana Alerting) 추가 → Discord/Slack/email 통지. **메트릭은 이미 다 있으니 룰만 얹으면 됨.** 최소 룰:
1. `increase(kraft.collect.auto.error[1h]) > 0`
2. `kraft.api.circuit_breaker.state == 2`(open) 지속 N분
3. readiness probe down
4. `kraft.api.fallback.exhausted` 발생(양 소스 모두 실패)
5. **신선도 SLO**: 최신 `winning_numbers.draw_date`가 기대 회차 대비 X시간 초과 지연

### O-3. 관측 스택 상시 구동 `P1` (운영)
**현황(confirmed)** — `prometheus`/`grafana`가 compose `profiles: [observability]` → 기본 `docker compose up`에서 **미구동**. 앱은 prod에서 `/actuator/prometheus`를 노출하지만 스크래퍼가 안 떠 있으면 메트릭 소비는 0.
**리스크** — 기본 배포 시 메트릭·대시보드·알림이 전부 비활성 → **로그만으로 운영**(O-2와 복합).
**조치** — prod 배포에 observability 프로파일을 **상시 포함**(또는 외부 수집기로 remote-write 푸시). prometheus.yml 주석대로 prometheus 컨테이너 IP를 `KRAFT_SECURITY_ACTUATOR_ALLOWED_IPS`에 포함(설정 결합 자동화).

### O-4. 백업 자동화·오프사이트·복구 드릴 `P0` (운영)
**현황(confirmed)** — `scripts/db-backup.sh`는 로컬 `/var/backups`에 `mysqldump --single-transaction | gzip`, 7일 보존. `db-restore.sh`도 존재. 그러나 **스케줄 없음, 오프사이트 없음, 복구 검증 절차 없음**(수동 실행 스크립트).
**리스크** — 백업이 **동일 호스트에만** 존재 → 호스트 손실 시 백업도 손실(O-1과 결합 시 **데이터 영구 손실**, 복구 불가). 복구가 실제 되는지 미검증.
**조치** — (a) **systemd timer/cron**으로 정기 백업, (b) **오프사이트 복제**(rclone→S3/오브젝트 스토리지/원격), (c) **분기 복구 드릴**(별도 환경에 restore + 행수·체크섬 검증), (d) 백업 성공/지연 메트릭 → 알림(O-2 연계).

### O-5. 리버스 프록시 / TLS를 코드로 `P1` (infra)
**현황(confirmed/infra)** — 저장소에 nginx/Caddy/TLS/certbot **없음**(`server-hardening.sh`만 매칭 — SSH/fail2ban용). 앱은 `127.0.0.1:8080` 바인드 → 호스트 외부 프록시(추정 Nginx)가 TLS·도메인 처리. 즉 **배포가 자기완결적이지 않고 프록시·인증서가 형상관리 밖**.
**리스크** — 재해 복구/재배포 시 프록시·TLS 수동 재구성 → 휴먼에러·드리프트, **인증서 만료 사고** 가능.
**조치** — compose에 **Caddy(자동 TLS)** 또는 nginx+certbot 서비스를 추가해 도메인·HTTPS·gzip/br·origin 보호까지 codify. (HSTS는 이미 앱 prod에서 true.) origin은 CDN/프록시 IP만 허용.

### O-6. 배포 러너 SPOF·보안 `P2` (infra)
**현황(confirmed)** — `cd.yml` `runs-on: [self-hosted, linux, x64]`, 로컬 compose 빌드/구동, `chown $GITHUB_WORKSPACE` → 정황상 **prod 호스트에 러너 동거**.
**리스크** — 러너 = 배포 단일점 + **러너 침해 시 prod 직접 노출**. 호스트 다운 시 배포 불가.
**조치** — C-1(build-once)로 prod 빌드를 제거한 뒤, 러너를 **별도 경량 호스트**로 분리하거나 전용 **비특권 사용자 + 네트워크 제한**. GHCR pull만 하면 러너 권한 최소화 가능.

### O-7. CDN / 정적 오프로드 부재 `P2` (infra)
**현황** — 정적 자산을 앱이 직접 서빙(content-hash 캐싱은 양호). 앞단 CDN 없음.
**리스크** — 단일 호스트가 정적+동적+TLS를 모두 처리 → 트래픽 급증/DDoS에 취약, 대역·CPU 낭비.
**조치** — Cloudflare(무료 티어) 등 CDN 프론트 → 정적 캐시·TLS·기본 WAF/DDoS 완화. origin은 CDN IP allowlist로 보호.

### O-8. 상류 의존(dhlottery) 취약성 `P2` (운영)
**현황(confirmed)** — `application.yml`에서 dhlottery를 브라우저 **UA/Referer 스푸핑**(`user-agent=Mozilla...`, `referer=...gameResult.do`)으로 호출. 302/HTML 차단 빈번(코드의 `HTML_UPSTREAM_BLOCKED`). 데이터 신선도가 이 불안정 상류에 종속.
**리스크** — 상류가 차단/구조 변경 시 **즉시 수집 중단**. UA 스푸핑은 ToS-gray.
**조치** — 다중 소스(B-1 폴백 smok) + 신선도 SLO·알림(O-2). 가능하면 안정적 데이터 출처 추가 확보. 상류 응답 구조 변경 감지용 **계약 테스트**(고정 픽스처 대비 스키마 검증).

### O-9. 시크릿 로테이션·관리 `P2` (운영)
**현황** — GitHub Actions secrets → `render-env.sh`로 `.env` 생성, 배포 후 `.env` 정리(양호). 자동 로테이션·이력 없음.
**리스크** — 장기 미회전 시 노출 영향 범위 큼(DB 비번/ops 토큰). (메모리상 노출 비밀 회전 계획이 있었음.)
**조치** — 정기 로테이션 절차 문서+자동화, ops 토큰/DB 비번 분리, **최소권한 DB 계정**(앱용 계정은 DDL 불가, Flyway만 별도 권한).

### O-10. 인시던트 런북 부재 `P3` (운영)
**현황** — README는 로컬 개발 기준. 배포/롤백 스크립트는 있으나 **장애 시나리오 playbook 부재**.
**조치** — `RUNBOOK.md`: "수집 실패 / 회로 open / DB 디스크 풀 / 컨테이너 OOM / 롤백 / 백업 복구" 각 대응 절차. 알림(O-2) → 대응 절차 링크.

### O-11. 디스크·호스트 리소스 모니터링 `P3` (운영)
**현황** — compose 로그 회전은 설정됨(json-file max-size). 이미지/백업 누적과 **호스트 디스크/메모리 모니터링·알람은 미확인**.
**조치** — node_exporter + 디스크/메모리 알림, 이미지 prune(C-7), 백업·로그 디스크 사용률 알림.

### O-12. 용량·부하 검증 부재 `P3` (운영)
**현황** — HikariCP prod max 20, JVM `MaxRAMPercentage=75`+G1GC, 가상스레드 on. 부하 검증은 성능 "스모크"(@perf)만.
**리스크** — 실제 트래픽 급증 시 풀 고갈/힙 압박 미검증.
**조치** — k6/Gatling 부하 프로파일로 p95·풀·GC 관찰, 튜닝 근거 확보. (트래픽이 작으면 우선순위 낮음.)

---

## 2. 백엔드 (Backend)

### B-1. 외부 수집 — 소스 자동 페일오버 `P0`
**현황(confirmed)** — `LottoApiClientConfig`가 `kraft.api.client` 토큰으로 **단일 구현 1개만** 빈 생성(prod 기본 `real`=DhLottery). `LottoApiClient.fetch(int)`는 `Optional<WinningNumber>`(빈=미추첨, 권위) 반환, 실패 시 `LottoApiClientException`/`CircuitBreakerOpenException` throw. **폴백 체이닝 없음**(grep 확인). 토·일·월 3중 크론이 모두 같은 단일 소스라 dhlottery 차단 시 3회 모두 실패.

**설계 — `CompositeLottoApiClient`(신규, 얇은 데코레이터)**
```java
final class CompositeLottoApiClient implements LottoApiClient {
    private final LottoApiClient primary, fallback;
    private final String primaryName, fallbackName;
    private final MeterRegistry meterRegistry;
    /* ctor 생략 */
    @Override public Optional<WinningNumber> fetch(int round) {
        try {
            return primary.fetch(round);                 // empty == 미추첨(권위) → 폴백 안 함
        } catch (CircuitBreakerOpenException | LottoApiClientException primaryFailure) {
            meterRegistry.counter("kraft.api.fallback.used","from",primaryName,"to",fallbackName).increment();
            try { return fallback.fetch(round); }
            catch (RuntimeException fallbackFailure) {
                meterRegistry.counter("kraft.api.fallback.exhausted").increment();
                fallbackFailure.addSuppressed(primaryFailure);
                throw fallbackFailure;                   // 둘 다 실패 → 기존 failed 집계 경로 유지
            }
        }
    }
}
```
**설정** — `KraftApiProperties`에 `String fallbackClient`(nullable, `@NotBlank` 아님) 추가. `application-prod.yml`에 `kraft.api.fallback-client: ${KRAFT_API_FALLBACK_CLIENT:smok}`(기타 프로파일 기본 비활성 → 완전 하위호환). `LottoApiClientConfig`는 토큰→클라이언트 생성을 `buildClient(token,...)`로 추출(각자 회로차단기 등록) 후, 폴백 토큰이 있고 primary와 다르면 Composite로 래핑.
**동작 규칙** — primary가 `Optional.empty`(미추첨)면 폴백 안 함, `circuit_open`/예외에만 폴백, 둘 다 실패 시 예외 전파(primary는 suppressed로 보존).
**테스트** — primary 성공→폴백 미호출 / empty→폴백 미호출 / 예외→폴백 성공+`fallback.used` / circuit_open→폴백 / 양쪽 실패→전파+`fallback.exhausted` / fallback 미설정·동일토큰→Composite 미생성. (`KraftApiPropertiesValidationTest`·`KraftPropertiesBindingTest`가 레코드 직접 생성 시 새 인자 반영.)

### B-2. H2 — prod 런타임 의존성 제거 `P1`
**근거(confirmed)** — `build.gradle.kts:51` `runtimeOnly("com.h2database:h2")`가 H2를 **prod `app.jar`에 동봉**. H2는 테스트 전용이며 `:64` `testRuntimeOnly`로 충분.
```diff
- runtimeOnly("com.h2database:h2")     // 51행 삭제 (prod jar에서 H2 제거)
  testRuntimeOnly("com.h2database:h2")  // 유지
```
회귀 위험 낮음: Flyway/DDL은 `FlywayMigrationIntegrationTest`(Testcontainers MariaDB)가, E2E는 `playwright.config.js`가 H2를 **명령행 주입**으로 검증.

### B-3. `lotto_fetch_logs` — 중복 인덱스 정리 `P1`
**근거(confirmed)** — `V1__baseline.sql`에 인덱스 7개. `(fetched_at)` ⊂ `(fetched_at,id)`, `(drw_no)` ⊂ `(drw_no,status)`. 보존 삭제(`findIdsByFetchedAtBefore` = `where fetchedAt<:cutoff order by id`)는 `(fetched_at,id)`로 완벽 커버, 실패 조회는 `(status,failure_reason,fetched_at)`로 커버. 단독 인덱스는 고빈도 INSERT에 쓰기 증폭.
```sql
-- V2__trim_fetch_log_indexes.sql  (EXPLAIN으로 단독 사용 없음 확인 후)
DROP INDEX idx_lotto_fetch_logs_fetched_at ON lotto_fetch_logs;
DROP INDEX idx_lotto_fetch_logs_drw_no     ON lotto_fetch_logs;
```

### B-4. 통계 캐시 무효화 — 이미 구현됨(선택적 하드닝) `P3`
**현황(confirmed)** — `WinningStatisticsService.evictCachesOnCollected`(`@Async @EventListener`)가 `dataChanged()` 시 요약 테이블 갱신 후 6개 캐시 evict(갱신 실패 시 TTL 폴백). **무효화는 견고하다.** 하드닝(필요 시): 이벤트가 트랜잭션 내부 발행으로 바뀌면 `@TransactionalEventListener(AFTER_COMMIT)`로, `@Async` 실행기가 `AsyncConfig`의 바운드 풀인지 확인.

### B-5. 추천 엔진 — 시도/타임아웃 가시화 `P2`
`max-attempts=5000`/`initial=10000`/`fixup=1000` + `RecommendGenerationTimeoutException` + `RecommendMetricsRecorder` 존재. 규칙 임계가 빡세지면 평균 시도수·p95 급증 가능. 이미 기록 중인 지표(시도 분포·타임아웃·fixup)를 Grafana 패널화 + 타임아웃율 알림(O-2 연계).

### B-6. 거대 클래스·자체 구현 정리 `P2` (코드 스멜)
- `WinningStatisticsCacheService`(14.5KB)가 빈도/기간빈도/궁합/패턴/조합이력/요약을 한 클래스에 집약 → 책임 분리(빈도·패턴·궁합) 검토로 가독성/테스트 격리.
- 자체 `ApiCircuitBreaker`/`ApiRetrySupport`는 유지보수 부담. 벌크헤드/타임리미터 표준화가 필요해지면 Resilience4j 검토(현재는 충분).
- `web/` 컨트롤러 24개 — Ops* 응집도 점검(과분할 여부).

### B-7. JaCoCo 임계 현실화 검토 `P3`
`jacocoTestCoverageVerification`이 class 97% / method 88% / line 82% / branch 65%. 품질 강제 장점이나 **사소 변경에도 커버리지 마찰**. 의도면 유지, 아니면 class/method를 90/85로 완화해 개발 속도 회복.

---

## 3. 프론트엔드 (Frontend)

### F-1. Bootstrap CSS 슬리밍 + 미사용 webjar 제거 `P1`
`base.html`이 `/vendor/bootstrap/bootstrap.min.css`(**232KB 풀 번들**)를 `<head>` 렌더블로킹으로 로드. `build.gradle.kts`의 `org.webjars:bootstrap:5.3.3`은 템플릿 **미참조**(grep 0). → (a) 사용 컴포넌트만 SCSS import 또는 PurgeCSS로 **30~60KB(~75%↓)**, htmx 동적 클래스는 safelist, (b) webjar 의존성 제거. Lighthouse/Playwright로 전후 검증.

### F-2. 앱 CSS minify(+선택 concat) `P2`
`app.css`(20KB)+`kraft-redesign.css`(26KB)+`lotto-ball.css`(13KB) 비압축 추정. content-hash 캐싱은 이미 양호 → cssnano minify 우선(HTTP/2면 concat 이득 작음).

### F-3. 구조화 데이터(JSON-LD) + sitemap `lastmod` `P1` — **CSP nonce 필수**
canonical/OG/twitter는 완비, **JSON-LD 전무**(grep 0), sitemap `<lastmod>` 없음. **중요**: 현재 CSP `script-src 'self'`가 인라인 스크립트를 차단 → JSON-LD 삽입은 **nonce 동반 필수**.
1. `SecurityHeadersFilter`에서 요청별 nonce 생성 → CSP를 `script-src 'self' 'nonce-<v>'`로 조립, request attr 노출.
2. `base.html`에 `<script type="application/ld+json" th:attr="nonce=${cspNonce}">`로 `WebSite`/`Organization` JSON-LD(회차/통계엔 `Dataset`/`BreadcrumbList`).
3. `SeoController`에 최신 회차 추첨일 주입 → 각 `<url>`에 `<lastmod>`(`WinningNumberQueryService.latestDrawDate()`; 없으면 `findTopByOrderByRoundDesc` 추가).

### F-4. 스크립트 로딩 `P3`
모든 `<script>` body 끝(논블로킹), `theme-init.js`만 head(FOUC 방지, 유지). `defer` 추가는 한계효용.

### F-5. PWA 아이콘 보강 `P3`
`manifest.json` 있음, 아이콘 `favicon.svg`만 → `apple-touch-icon`(PNG 180) + manifest PNG(192/512).

### F-6. 접근성 `P2`
`aria-current`/`aria-live`/`visually-hidden`/`aria-pressed` 양호 + `@axe-core/playwright` 보유. 추가: skip-to-content 링크, 색대비 axe 상시 점검, 데스크톱 navbar 토글 부재(모바일 하단 nav 대체) 의도 확인.

---

## 4. CI / CD

> 현 상태 — CI: `check bootJar` + jar검증 + Trivy/SBOM(비-PR) + Playwright 스모크 + 성능 스모크. CD: workflow_run/수동 → **self-hosted 러너에서 소스 재빌드 후 compose up** → 프로필검증 → readiness → 롤백 → smoke. 성숙하나 아래 여지.

### C-1. [CD] build-once: CI 푸시 → CD digest pull `P1`
**문제(confirmed)** — CD가 prod 호스트에서 jar+이미지를 **재빌드**(`build-and-up.sh`). CI 산출물과 별개 → 배포 지연·prod CPU 점유·**공급망 불일치**.
**조치** — CI에 GHCR 푸시(`ghcr.io/portuna85/kraft-lotto-app:${sha}`) 추가, CD는 `compose build`를 **`compose pull`**로 교체(GHCR 로그인). compose의 `KRAFT_APP_IMAGE_REF/TAG` 활용. → **테스트된 그 이미지** 그대로 배포(불변·재현).

### C-2. [CI] Gradle 캐시 고도화 `P1`
`actions/setup-java cache:gradle` → `gradle/actions/setup-gradle@v4`(의존성/설정 캐시 + 적중 리포트 + build scan). `gradle.properties`에 `org.gradle.configuration-cache=true`(커스텀 태스크 CC 호환 1회 검증). 반복 빌드 단축.

### C-3. [CI] PR 보안 보강 `P1`
Trivy/Syft가 비-PR 조건이라 **PR은 스캔 skip**. → PR에 `actions/dependency-review-action@v4` + Trivy `fs`. 머지 전 공급망 회귀 차단.

### C-4. [CI] CodeQL SAST `P2`
보안 특화 SAST 없음. `.github/workflows/codeql.yml`(`java-kotlin`, PR+schedule) 추가. (대안: SpotBugs `find-sec-bugs` 플러그인.)

### C-5. [CI] 빌드 프로비넌스 / 미사용 권한 정리 `P2`
`build-test`가 `id-token/attestations: write`를 **선언만 하고 미사용**. → `actions/attest-build-provenance@v2`로 jar·이미지(C-1) 프로비넌스 생성, 안 쓸 거면 권한 제거(최소권한).

### C-6. [CI] 정적분석 병렬 잡 분리 `P2` (선택)
`check`가 test+checkstyle+spotbugs 묶음 → checkstyle/spotbugs 별도 잡 분리로 신호 가속(C-2 캐시 적용 후 유리).

### C-7. [CD] self-hosted 디스크 prune `P2`
배포마다 이미지/레이어 누적 → 말미에 `docker image prune -f`(+`builder prune`) 추가로 디스크 상한(O-11 연계).

### C-8. [CD] 환경 승인 게이트 `P3` (선택)
`workflow_run`이 main CI 성공마다 자동 배포(연속 배포). 통제 필요 시 `production` 환경에 수동 승인(required reviewers).

### C-9. 액션 SHA 핀고정 + Dependabot `P3`
서드파티 액션이 `@v5/@v7`(가변 태그) → 커밋 SHA 핀 + `.github/dependabot.yml`(actions/gradle/npm/docker)로 유지보수 자동화.

---

## 5. 종합 우선순위표 (전 항목)

| ID | 유형 | 항목 | 우선순위 | 리스크/효과 |
| --- | --- | --- | --- | --- |
| **O-2** | 운영 | 능동 알림(Alertmanager/통지) | **P0** | 장애를 늦게 인지 → 회차 누락 방치 |
| **B-1** | 백엔드 | 소스 자동 페일오버 | **P0** | 단일 소스 차단 시 수집 정지 |
| **O-4** | 운영 | 백업 자동화·오프사이트·복구드릴 | **P0** | 데이터 영구 손실 가능 |
| **O-1** | 인프라 | 단일 호스트 SPOF(DB/러너 분리) | **P1** | 호스트 1대 = 전면 다운 |
| **O-5** | 인프라 | 리버스 프록시/TLS IaC | **P1** | 인증서 만료·재구성 드리프트 |
| **O-3** | 운영 | 관측 스택 상시 구동 | **P1** | 기본 배포 시 메트릭/알림 비활성 |
| **C-1** | CI/CD | build-once(이미지 푸시→pull) | **P1** | 배포 지연·공급망 불일치 |
| **F-1** | 프론트 | Bootstrap 슬리밍 + webjar 제거 | **P1** | 최초 렌더 232KB 블로킹 |
| **B-2** | 백엔드 | H2 prod 제거 | **P1** | 산출물 비대·공격면 |
| **B-3** | 백엔드 | fetch_logs 중복 인덱스 정리 | **P1** | 쓰기 증폭·스토리지 |
| **F-3** | 프론트 | JSON-LD + lastmod(+CSP nonce) | **P1** | 색인/노출 저조 |
| **C-2** | CI/CD | Gradle 캐시 + config cache | **P1** | 빌드 시간 |
| **C-3** | CI/CD | PR 보안 스캔 | **P1** | 머지 전 공급망 회귀 |
| **O-6** | 인프라 | 배포 러너 분리·보안 | P2 | 러너 침해=prod 노출 |
| **O-7** | 인프라 | CDN/정적 오프로드 | P2 | 트래픽·DDoS 취약 |
| **O-8** | 운영 | 상류 의존 취약성 관리 | P2 | 상류 변경 시 수집 중단 |
| **O-9** | 운영 | 시크릿 로테이션 | P2 | 노출 시 영향 큼 |
| **C-4** | CI/CD | CodeQL SAST | P2 | 보안 결함 미탐 |
| **C-5** | CI/CD | 빌드 프로비넌스/권한 | P2 | 공급망 신뢰 |
| **C-7** | CI/CD | self-hosted prune | P2 | 디스크 증가 |
| **B-5** | 백엔드 | 추천 시도/타임아웃 관측 | P2 | p95 지연 미가시 |
| **B-6** | 백엔드 | 거대 클래스·자체구현 정리 | P2 | 유지보수성 |
| **F-2** | 프론트 | 앱 CSS minify | P2 | 자산 용량 |
| **F-6** | 프론트 | 접근성 점검 루틴 | P2 | a11y 회귀 |
| **C-6** | CI/CD | 정적분석 병렬 잡 | P2 | 신호 속도 |
| **O-10** | 운영 | 인시던트 런북 | P3 | 대응 표준화 |
| **O-11** | 운영 | 디스크/리소스 모니터링 | P3 | 디스크 풀 사고 |
| **O-12** | 운영 | 부하 검증 | P3 | 용량 미검증 |
| **B-4** | 백엔드 | 캐시 무효화(이미 구현·하드닝) | P3 | 변경 불요 권장 |
| **B-7** | 백엔드 | JaCoCo 임계 현실화 | P3 | 개발 마찰 |
| **F-4** | 프론트 | script defer | P3 | 한계효용 |
| **F-5** | 프론트 | PWA 아이콘 | P3 | 설치 경험 |
| **C-8** | CI/CD | 환경 승인 게이트 | P3 | 배포 통제 |
| **C-9** | CI/CD | 액션 SHA 핀 + Dependabot | P3 | 공급망 |

---

## 6. 권장 진행 순서 (스프린트)

1. **S1 — "장애를 알게 하고, 데이터를 지킨다"**: O-2(알림) → O-4(백업 자동화/오프사이트) → O-3(관측 상시). *코드 변경 거의 없이 운영 점수가 가장 크게 오르는 구간.*
2. **S2 — 가용성/단일점 해소**: B-1(소스 페일오버) → O-1(DB 분리) → O-5(프록시/TLS IaC) → C-1(build-once) → O-6(러너 분리).
3. **S3 — 저위험 최적화**: B-2 → B-3 → C-2 → C-3.
4. **S4 — 체감/노출**: F-1 → F-3 → C-4/C-5 → O-7(CDN).
5. **S5 — 정제**: B-5/B-6/B-7 / F-2/F-6 / O-8~O-12 / C-6~C-9.

## 7. 회귀 검증 체크리스트
- [ ] `./gradlew check bootJar -PstrictStatic=true -PstrictCoverage=true` 통과
- [ ] `FlywayMigrationIntegrationTest`(Testcontainers MariaDB) — B-2/B-3
- [ ] Playwright 스모크 + Lighthouse — F-1/F-3
- [ ] B-1: 폴백 6시나리오 + `fallback.used/exhausted` 메트릭 노출
- [ ] O-2: 알림 룰 발화 테스트(수집 실패/circuit open/신선도 지연을 인위 유발 → 통지 수신)
- [ ] O-4: 오프사이트 백업에서 **실제 복구** 후 행수·체크섬 검증(드릴)
- [ ] F-3: CSP nonce 반영 후 브라우저 콘솔 CSP violation 0
- [ ] C-1: CD가 GHCR `:${sha}` 이미지를 pull, 컨테이너 `SPRING_PROFILES_ACTIVE=prod` 검증
- [ ] 배포 후 `/actuator/health/readiness` + 공개 라우트 smoke

---

## 부록 — 비기술 약점(정직한 메모)
엔지니어링 성숙도(회로차단기·14종 실패분류·속성/성능 테스트·SBOM)에 비해 **제품 견인이 매우 낮다**(검색 노출·클릭 거의 0 수준으로 파악됨). "운영"의 궁극 목적은 사용자가 쓰는 것이므로, 위 기술 백로그와 별개로 **(1) 실제 사용자/유입이 있는지, (2) 그렇다면 SEO·콘텐츠(F-3 포함)·공유 동선에 노력 일부를 재배분할지**를 함께 판단할 가치가 있다. 학습·포트폴리오 목적이라면 현재의 기술 집중은 합리적이다 — 목적을 명확히 하는 것이 우선순위의 전제다.
