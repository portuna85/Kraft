# 프론트엔드 실서비스 리팩토링 개선 항목

> 기준: 2026-06-14  
> 범위: `web/` 전체 — 운영 중인 구 사이트(kraft.io.kr)의 장점 반영 + demo 프로젝트 강점 극대화

---

## 1. next-env.d.ts 타입 경로 수정

**파일:** `web/next-env.d.ts`

```diff
- import "./.next/dev/types/routes.d.ts";
+ import "./.next/types/routes.d.ts";
```

**이유:** `.next/dev/types/` 경로는 개발 서버(next dev)에서만 생성됨. 프로덕션 빌드(`next build`)에서는 `.next/types/`를 참조해야 타입 오류 없이 빌드됨.

---

## 2. 홈페이지 ISR 전환

**파일:** `web/src/app/page.tsx`

```diff
- export const dynamic = "force-dynamic";
+ export const revalidate = 60;
```

**이유:**
- `force-dynamic`은 모든 요청을 SSR로 처리 → 서버 부하 증가, TTFB 증가
- 로또 추첨은 주 1회. 실시간성이 없어도 됨
- 백엔드의 `RevalidateWebhookListener` + 프론트의 `/api/revalidate` 엔드포인트로 신규 회차 수집 즉시 on-demand ISR 갱신 가능
- `revalidate=60`은 webhook 실패 시 fallback 역할만 수행

---

## 3. 세금 계산기 (순수 프론트)

**신규 파일:** `web/src/lib/tax.ts`  
**적용:** `latest/page.tsx`, `rounds/[round]/page.tsx`

**계산 기준 (소득세법 제129조):**
| 당첨금 | 세율 | 비고 |
|--------|------|------|
| 3억 초과 | 33% | 소득세 30% + 지방소득세 3% |
| 200만 초과 ~ 3억 이하 | 22% | 소득세 20% + 지방소득세 2% |
| 200만 이하 | 비과세 | — |

**표시 위치:**
- 최신 회차(`/latest`): 1등 당첨금 아래 세후 예상 수령액
- 회차 상세(`/rounds/[round]`): round-detail-grid에 세후 수령액 셀 추가

---

## 4. 회차 번호 검색

**신규 컴포넌트:** `web/src/components/round-search-form.tsx`  
**적용:** `web/src/app/rounds/page.tsx` 상단

**기능:**
- 회차 번호 직접 입력 → `/rounds/:round` 로 이동
- 유효하지 않은 입력(비숫자, 0 이하) 제출 차단
- 기존 페이지네이션 목록과 공존 (추가만, 대체 아님)

---

## 5. 동반 출현 번호별 필터

**신규 컴포넌트:** `web/src/components/companion-filter-client.tsx`  
**적용:** `web/src/app/companion/page.tsx`

**기능:**
- 1~45 번호 선택 버튼 표시 (기존 ball 색상 CSS 재사용)
- 선택한 번호가 `ballA` 또는 `ballB`에 포함된 쌍만 필터
- 선택 해제 시 전체 TOP 50 표시 (기존 동작 유지)
- API 호출은 서버 컴포넌트에서 ISR 1800s 유지 — 클라이언트 필터링으로 추가 API 없음

---

## 6. 한국어 Copy 전면 개선 (24개 파일)

**목표:** 사용자 중심의 자연스러운 문구로 통일

| 항목 | 기존 | 개선 |
|------|------|------|
| 홈 h1 | 로또 6/45 당첨번호 조회와 번호 추천을 한 번에 | 최신 로또 당첨 결과와 내 번호 관리를 한 곳에서 |
| 메타 설명 | KST 기준 최신 로또 회차, 번호 추천, 저장함 | 로또 당첨 결과 조회, 번호 추천과 저장까지 한 곳에서 |
| 네비게이션 | 최신 회차 / 회차 목록 / 빈도 / 패턴 / 동반 | 최신 결과 / 전체 회차 / 출현 빈도 / 패턴 통계 / 동반 출현 |
| 에러 페이지 제목 | 잠시 후 다시 시도해 주세요 | 페이지를 불러오지 못했습니다 |
| 저장함 eyebrow | 저장 번호 | 내 번호 |
| OG 이미지 | 로또 번호 조회 · 추천 · 저장 | 당첨 결과 조회 · 추천 조합 · 번호 저장 |

---

## 7. demo 프로젝트 강점 유지 목록

이번 리팩토링에서 기존 구현을 변경 없이 그대로 활용한 강점:

| 항목 | 구현 위치 |
|------|----------|
| CSP nonce 미들웨어 (per-request) | `web/src/middleware.ts` |
| on-demand ISR webhook | `web/src/app/api/revalidate/route.ts` |
| JSON-LD 구조화 데이터 | `web/src/components/json-ld.tsx` |
| 접근성 (skip-nav, aria-current, focus-visible) | `layout.tsx`, `nav-links.tsx` |
| 다크모드 CSS 변수 완전 지원 | `globals.css` |
| SEO (sitemap, robots, 동적 메타데이터) | `app/sitemap.ts`, `robots.ts` |
| X-Device-Token 익명 기기 식별 | `saved-numbers-client.tsx` |
| BackendError 타입 + 5초 타임아웃 | `lib/api.ts` |

---

## 구 kLo 사이트 참조 항목

| kLo 기능 | demo 반영 여부 | 비고 |
|----------|-------------|------|
| 세금 계산기 | ✅ 반영 (§3) | 순수 프론트 계산 |
| 회차 번호 검색 | ✅ 반영 (§4) | 클라이언트 router.push |
| 동반 번호 — 번호별 조회 | ✅ 반영 (§5) | 클라이언트 필터링 |
| 빈도 — 기간별 필터 | ⏳ 보류 | 백엔드 API 확장 필요 |
| FCM 푸시 알림 | ⏳ 보류 | Firebase 설정 필요 |
| Google Adsense | ⏳ 보류 | 수익화 정책 결정 후 |
