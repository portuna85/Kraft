# 관리자 접속 가이드

## 관리자 UI 접속

서버에 브라우저가 없으므로 SSH 터널을 이용합니다.

```bash
# 1. 내 PC에서 터널 열기
ssh -L 9090:localhost:8080 kraft@kraft.io.kr

# 2. 브라우저에서 접속
http://localhost:9090/admin/login
```

로그인 정보:
- 아이디: `admin`
- 비밀번호: GitHub `production` Secrets → `KRAFT_ADMIN_PASSWORD`

로그인 후 이동:

```
http://localhost:9090/admin/ops
```

---

## Ops API 접속

`/ops/**` 엔드포인트는 토큰 인증이 필요합니다.

```bash
# 토큰 값 확인 (서버에서)
cd ~/apps/kLo
docker compose exec -T app sh -lc 'printenv KRAFT_SECURITY_OPS_REQUIRED_TOKEN'

# API 호출
curl -s -H "X-Ops-Token: <토큰값>" http://localhost:8080/ops/fetch-logs/failure-overview
curl -s -H "X-Ops-Token: <토큰값>" http://localhost:8080/ops/circuit-breakers
```

토큰 원본: GitHub `production` Secrets → `KRAFT_SECURITY_OPS_REQUIRED_TOKEN`

---

## 미완료

- [ ] Ops API 토큰을 관리자 UI에서 확인하거나 갱신하는 기능 없음 — 현재는 GitHub Secrets 교체 후 CD 재실행 필요
