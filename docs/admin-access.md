# KRAFT Lotto 관리자 페이지 접속 가이드

## 접속 구조 개요

```
[내 PC 브라우저]
      │
      │ SSH 터널 (로컬 9090 → 서버 8080)
      ▼
[운영 서버: localhost:8080]  ← Caddy가 외부에서 /ops*, /admin/ops* 차단
      │
      ▼
[Spring Boot 앱]
  OpsAccessFilter: IP 허용 목록 + 토큰 검증
```

Caddy가 `/ops*`, `/admin/ops*` 경로를 외부에서 차단하므로,
**서버 내부(localhost)에서만 접근 가능**합니다.

---

## 1단계: SSH 터널 연결

터미널에서 아래 명령어를 실행합니다. **터널이 열려 있는 동안 터미널을 닫지 마세요.**

```bash
# 기본 (비밀번호 인증)
ssh -L 9090:localhost:8080 root@kraft.io.kr

# 키 파일 인증
ssh -L 9090:localhost:8080 -i ~/.ssh/kraft_key root@kraft.io.kr

# 백그라운드 실행 (터미널 입력 없이 터널만 유지)
ssh -fN -L 9090:localhost:8080 root@kraft.io.kr
```

터널이 성공하면 로컬의 `9090` 포트가 서버의 `8080`에 연결됩니다.

---

## 2단계: 운영 토큰 확인

서버에 SSH로 접속한 상태에서 아래 명령어로 토큰을 확인합니다.

```bash
docker compose exec -T app sh -lc 'printenv KRAFT_SECURITY_OPS_REQUIRED_TOKEN'
```

> 토큰이 비어 있으면(`""`) IP만 허용되면 접근 가능합니다.
> 터널을 통하면 `127.0.0.1`로 요청이 들어오므로 IP 조건은 자동 통과됩니다.

---

## 3단계: 브라우저로 대시보드 접속

SSH 터널이 열린 상태에서 브라우저 주소창에 입력합니다.

```
http://localhost:9090/admin/ops
```

토큰이 설정된 경우, 브라우저 확장 [ModHeader](https://chrome.google.com/webstore/detail/modheader-modify-http-hea/idgpnmonknjnojddfkpgkljpfnnfcklj)를 설치하고 헤더를 추가합니다.

| 헤더 이름 | 값 |
|---|---|
| `X-Ops-Token` | `<확인한 토큰>` |

---

## 관리자 엔드포인트 목록

### HTML 대시보드

| 경로 | 설명 |
|---|---|
| `GET /admin/ops` | 수집 실패 대시보드 (브라우저 전용) |

### REST API (curl 또는 Swagger)

#### 수집 관련 (`/ops/collect`)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/ops/collect/status` | 현재 수집 작업 실행 여부 확인 |
| `POST` | `/ops/collect` | 최신 회차까지 수집 실행 |
| `POST` | `/ops/collect/missing` | 누락 회차만 재수집 |
| `POST` | `/ops/collect/stores?round=1226` | 특정 회차 판매점 수집 |

#### 수집 로그 (`/ops/fetch-logs`)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/ops/fetch-logs/failure-reasons` | 수집 실패 사유 요약 |
| `GET` | `/ops/fetch-logs/failures` | 최근 수집 실패 목록 |
| `GET` | `/ops/fetch-logs/failure-overview` | 실패 사유 + 목록 통합 조회 |
| `GET` | `/ops/fetch-logs/retention-status` | 로그 보관 설정 및 삭제 대상 수 |

#### 모니터링 (`/ops`)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/ops/data-freshness` | DB 최신 회차 vs 예상 최신 회차 비교 |
| `GET` | `/ops/circuit-breakers` | 외부 API 서킷브레이커 상태 |
| `GET` | `/ops/recommend/stats` | 번호 추천 생성 통계 |

#### 뉴스 (`/ops/news`)

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/ops/news/collect` | 뉴스 수동 수집 트리거 |

---

## curl 사용 예시

SSH 터널이 열린 상태에서 별도 터미널로 실행합니다.

```bash
TOKEN="여기에-토큰-입력"

# 데이터 freshness 확인
curl -s -H "X-Ops-Token: $TOKEN" http://localhost:9090/ops/data-freshness | jq .

# 수집 상태 확인
curl -s -H "X-Ops-Token: $TOKEN" http://localhost:9090/ops/collect/status | jq .

# 최신 회차 수집 실행
curl -s -X POST -H "X-Ops-Token: $TOKEN" http://localhost:9090/ops/collect | jq .

# 누락 회차 재수집
curl -s -X POST -H "X-Ops-Token: $TOKEN" http://localhost:9090/ops/collect/missing | jq .

# 특정 회차 판매점 수집 (예: 1226회)
curl -s -X POST -H "X-Ops-Token: $TOKEN" \
  "http://localhost:9090/ops/collect/stores?round=1226" | jq .

# 서킷브레이커 상태
curl -s -H "X-Ops-Token: $TOKEN" http://localhost:9090/ops/circuit-breakers | jq .

# 실패 사유 요약
curl -s -H "X-Ops-Token: $TOKEN" http://localhost:9090/ops/fetch-logs/failure-reasons | jq .

# 뉴스 수동 수집
curl -s -X POST -H "X-Ops-Token: $TOKEN" http://localhost:9090/ops/news/collect | jq .
```

---

## 서버 직접 실행 (SSH 내부에서)

SSH 접속 후 서버 내부에서 바로 실행할 수도 있습니다.

```bash
# 서버에서 토큰 읽기
TOKEN=$(docker compose exec -T app sh -lc 'printenv KRAFT_SECURITY_OPS_REQUIRED_TOKEN')

# 데이터 freshness 확인
curl -s -H "X-Ops-Token: $TOKEN" http://localhost:8080/ops/data-freshness | jq .

# 최신 회차 수집
curl -s -X POST -H "X-Ops-Token: $TOKEN" http://localhost:8080/ops/collect | jq .
```

---

## 접속 확인 방법

터널이 정상인지 먼저 확인합니다.

```bash
# 앱 헬스체크 (토큰 불필요)
curl http://localhost:9090/actuator/health/readiness
# 예상 응답: {"status":"UP"}
```

---

## 주의사항

- `/admin/ops`와 `/ops/*`는 Caddy에서 외부 접근을 **403**으로 차단합니다.
- SSH 터널 없이 `https://www.kraft.io.kr/admin/ops`에 직접 접근하면 **403**이 반환됩니다.
- `POST /ops/collect`는 실제 DB에 데이터를 쓰므로 운영 중 신중하게 사용하세요.
- Bearer 토큰 방식도 지원합니다: `Authorization: Bearer <토큰>`
