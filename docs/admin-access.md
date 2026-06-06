# 관리자 로그인 활성화 및 접속 가이드

## 전제 조건

- 서버: Ubuntu Server 26.04 LTS (브라우저 없음)
- 배포: GitHub Actions CD 워크플로우가 담당
- 앱 환경변수: 배포 시 생성되는 `.env` 파일로 주입

---

## 1단계 — GitHub 환경 값 등록

GitHub 저장소 → **Settings → Environments → production** 으로 이동합니다.

### Variable 추가

| 이름 | 값 |
|---|---|
| `KRAFT_ADMIN_ENABLED` | `true` |

### Secret 추가

| 이름 | 값 |
|---|---|
| `KRAFT_ADMIN_PASSWORD` | 강한 비밀번호 |
| `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` | 강한 랜덤 토큰 |

비밀번호 / 토큰 기준: 영문 대소문자 + 숫자 + 특수문자 조합, 충분한 길이.  
피해야 할 값: `admin`, `1234`, `password`, 사이트 이름, 짧은 문자열.

> **주의:** 이 값들은 모두 **Secrets** 에 넣어야 합니다. Variables 에 넣지 마세요.

---

## 2단계 — CD 워크플로우 재실행

GitHub 값 저장만으로는 서버에 반영되지 않습니다. 배포를 다시 실행해야 합니다.

**방법 A — 수동 실행 (권장)**

```bash
gh workflow run cd.yml --repo portuna85/kLo --ref main \
  --field ref=$(git rev-parse HEAD)
```

또는 GitHub Actions 탭에서 **CD - Deploy to kraft-server** → **Run workflow** 선택.

**방법 B — 빈 커밋 push**

```bash
git commit --allow-empty -m "ci: CD 재배포 트리거"
git push origin main
```

> CI가 성공해야 CD가 자동 실행됩니다.  
> 빈 커밋은 파일 변경이 없어 CI가 트리거되지 않을 수 있으므로 방법 A를 권장합니다.

---

## 3단계 — 서버에서 배포 결과 확인

SSH로 서버에 접속한 뒤 순서대로 확인합니다.

```bash
# 앱 정상 기동 확인
curl http://localhost:8080/actuator/health/readiness
# 기대 결과: {"status":"UP"}

# 관리자 기능 활성화 확인
docker compose exec -T app sh -lc 'printenv KRAFT_ADMIN_ENABLED'
# 기대 결과: true

# 관리자 비밀번호 주입 확인 (값이 빈 줄이 아니어야 함)
docker compose exec -T app sh -lc 'printenv KRAFT_ADMIN_PASSWORD'

# 관리자 로그인 경로 응답 확인
curl -I http://localhost:8080/admin/login
# 기대 결과: HTTP/1.1 200
```

> 비밀번호 출력 후 터미널 화면 공유나 기록에 주의하세요.

---

## 4단계 — 내 PC에서 로그인 화면 확인

서버에는 브라우저가 없으므로 SSH 터널을 이용합니다.

**1. 내 PC에서 SSH 터널 열기**

```bash
ssh -L 9090:localhost:8080 kraft@kraft.io.kr
```

**2. 내 PC 브라우저에서 접속**

```
http://localhost:9090/admin/login
```

**3. 로그인**

- 아이디: `admin`
- 비밀번호: GitHub Secret `KRAFT_ADMIN_PASSWORD` 에 등록한 값

**4. 로그인 후 관리 화면**

```
http://localhost:9090/admin/ops
```

---

## Ops API 직접 호출

`/ops/**` REST 엔드포인트는 `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` 토큰을 요구합니다.  
토큰 없이 호출하면 **401 Unauthorized** 가 반환됩니다.

서버에서 호출할 때는 아래처럼 헤더를 포함합니다.

```bash
# X-Ops-Token 헤더 방식
curl -s -H "X-Ops-Token: <토큰값>" \
  http://localhost:8080/ops/fetch-logs/failure-overview

# Bearer 토큰 방식
curl -s -H "Authorization: Bearer <토큰값>" \
  http://localhost:8080/ops/circuit-breakers
```

토큰 값은 GitHub **Settings → Environments → production → Secrets → `KRAFT_SECURITY_OPS_REQUIRED_TOKEN`** 에서 확인합니다.

---

## 최종 체크리스트

- [ ] GitHub `production` Variables 에 `KRAFT_ADMIN_ENABLED=true` 등록
- [ ] GitHub `production` Secrets 에 `KRAFT_ADMIN_PASSWORD` 등록
- [ ] GitHub `production` Secrets 에 `KRAFT_SECURITY_OPS_REQUIRED_TOKEN` 등록
- [ ] CD 워크플로우 재실행
- [ ] `curl .../actuator/health/readiness` → `{"status":"UP"}`
- [ ] `printenv KRAFT_ADMIN_ENABLED` → `true`
- [ ] `printenv KRAFT_ADMIN_PASSWORD` → 빈 값 아님
- [ ] SSH 터널 연결
- [ ] 브라우저에서 `/admin/login` 접속 및 로그인 확인

---

## 문제 해결

### CD가 실패한다

GitHub Actions 로그에서 아래 메시지를 확인합니다.

```
KRAFT_ADMIN_PASSWORD GitHub Secret is required when KRAFT_ADMIN_ENABLED=true
```

→ `KRAFT_ADMIN_ENABLED=true` 인데 `KRAFT_ADMIN_PASSWORD` Secret 이 없는 상태입니다.  
→ 1단계로 돌아가 Secret 을 등록하고 재배포합니다.

### 앱은 UP인데 관리자 로그인이 안 된다

```bash
docker compose exec -T app sh -lc 'printenv KRAFT_ADMIN_ENABLED'
docker compose exec -T app sh -lc 'printenv KRAFT_ADMIN_PASSWORD'
curl -I http://localhost:8080/admin/login
```

세 가지를 다시 확인하고, 값이 올바르게 주입되었는지 점검합니다.

### `localhost:9090` 이 안 열린다

1. SSH 터널 명령(`ssh -L 9090:localhost:8080 kraft@kraft.io.kr`)을 실행했는지 확인
2. 서버에서 `curl http://localhost:8080/actuator/health/readiness` 가 응답하는지 확인
