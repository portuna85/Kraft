# CHANGELOG

## [Unreleased]

### 추가
- 뉴스 domain 기반 `source_domain` 컬럼 저장 (DB 필터링 지원)
- 판매점 수집 Micrometer 메트릭 (`kraft.winning_store.collected`, `kraft.winning_store.fetch.failure`)
- 최신 회차 lag Gauge 메트릭 (`kraft.latest_round.stored`, `kraft.latest_round.expected`)
- Prometheus 알림 규칙: `LottoLatestRoundLag`, `WinningStoreFetchFailure`
- `SECURITY.md` 보안 신고 절차 추가
- `/news?tier=general` 접근 시 `noindex,nofollow` meta 태그 적용
- sitemap에 `/news?tier=official`, `/news?tier=press` 항목 추가

### 변경
- 뉴스 기본 노출 범위를 OFFICIAL + PRESS tier로 제한 (GENERAL 기본 미노출)
- `kmrk.ru` 등 저품질 도메인 blocked-domains 설정 추가
- exclude-keywords 강화 (전세, 공익신고, 해외 로또, 파워볼 등)
- `README.md` 제목 "최고의 번호" → "KRAFT Lotto — 로또 번호 조합 분석 도구"
- `manifest.json` description 한국어 중립 문구로 변경
- `latest.html` 판매점 섹션에 `data-testid="store-collection-status"` 추가

### 테스트
- E2E smoke: 브랜드 문구 회귀, GENERAL 미노출, 판매점 상태 표시 테스트 추가

---

## [0.2.0] — 2026-06-01

### 추가
- CI: `strictStatic`, `strictCoverage`, Trivy HIGH/CRITICAL gating
- prod API `real` 우선 + `smok` fallback 구조 (`CompositeLottoApiClient`)
- `MAX_ROUND` 장기 운영 리스크 완화 (날짜 기반 계산)
- 신뢰 페이지: `/methodology`, `/data-source`, `/faq`, `/responsible-play`, `/privacy`, `/terms`, `/contact`
- Caddy `/actuator*`, `/ops*`, `/admin/ops*` 403 차단
- Public Rate Limit 확대: `/news`, `/recommend`, `/fragments/` 등 포함

---

## [0.1.0] — 2026-01-01

### 추가
- 로또 6/45 당첨번호 수집 및 조회
- 편향 회피 번호 조합 추천
- 빈도·패턴 통계 (`/frequency`, `/stats`)
- 번호 조합 분석 (`/analysis`, `/companion`)
- 뉴스 자동 수집 (Google News RSS)
- Prometheus + Grafana + Alertmanager 모니터링
- Docker Compose 운영 환경 구성
