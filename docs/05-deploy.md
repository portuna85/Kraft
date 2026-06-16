# CI/CD 파이프라인

## 전체 흐름

```
git push → main
     │
     ├─── CI (ci.yml) ──────────────────────────────────────────────────────►
     │      │
     │      ├─ Backend Build & Test     (JUnit, JaCoCo 커버리지)
     │      ├─ Web Build & Test         (TypeScript, ESLint, Vitest)
     │      ├─ Static Analysis          (SpotBugs, Checkstyle)
     │      ├─ Caddy Validate           (Caddyfile 문법 검사)
     │      ├─ Removed Feature Guard    (삭제된 기능 경로 보호)
     │      ├─ Docker Publish           (이미지 빌드 → GHCR 업로드)
     │      ├─ Security Scan            (Trivy 취약점 스캔)
     │      └─ Promote to latest        (:sha 태그 → :latest 태그)
     │
     └─── CodeQL (codeql.yml) ─────────────────────────────────────────────►
            보안 코드 분석 (주 1회 + push 시)

CI 성공 시
     │
     └─── CD (cd.yml) ──────────────────────────────────────────────────────►
            │
            ├─ [1/6] GHCR 로그인
            ├─ [2/6] 현재 이미지 참조 저장 (롤백용)
            ├─ [3/6] git reset --hard <SHA>
            ├─ [4/6] .env.prod 렌더링 (GitHub Secrets → 파일)
            ├─ [5/6] 이미지 pull + 서비스 재시작
            │        실패 시 → 자동 롤백 (이전 이미지로 복구)
            └─ [6/6] 배포 완료
```

---

## 워크플로우 파일

| 파일 | 트리거 | 역할 |
|------|--------|------|
| `.github/workflows/ci.yml` | push/PR → main | 빌드·테스트·이미지 빌드 |
| `.github/workflows/cd.yml` | CI 완료 후 | 서버 배포 |
| `.github/workflows/codeql.yml` | push → main, 주 1회 | 보안 코드 분석 |
| `.github/workflows/pr.yml` | PR → main | PR 검사 (CI 경량 버전) |

---

## 이미지 레지스트리 (GHCR)

CI가 빌드 후 아래 태그로 GHCR에 push한다:

```
ghcr.io/portuna85/kraft/backend:<git-sha>
ghcr.io/portuna85/kraft/backend:latest

ghcr.io/portuna85/kraft/web:<git-sha>
ghcr.io/portuna85/kraft/web:latest
```

확인:  
https://github.com/portuna85/Kraft/pkgs/container/kraft%2Fbackend

---

## 배포 서버 경로

| 항목 | 값 |
|------|-----|
| 서버 IP | `49.143.105.191` |
| 배포 경로 | `/srv/kraft` |
| 실행 compose | `docker-compose.prod.yml` |
| 환경변수 파일 | `/srv/kraft/.env.prod` (CD가 자동 생성) |

---

## 배포 중 서비스 영향

`docker compose up -d --remove-orphans` 사용으로 **변경된 서비스만** 재시작된다.  
MariaDB는 데이터 유지를 위해 이미지 변경이 없으면 재시작하지 않는다.

---

## 수동 배포 (긴급 시)

```bash
# 서버에서 직접 실행
cd /srv/kraft
git fetch --depth=1 origin main
git reset --hard origin/main
bash scripts/deploy/render-env.sh .env.prod.example .env.prod
bash scripts/deploy/pull-and-up.sh
```

---

## 배포 실패 시 자동 롤백

CD 워크플로우가 배포 실패를 감지하면 이전 이미지로 자동 롤백한다.

수동 롤백이 필요한 경우:
```bash
# 이전 이미지 SHA 확인 (로그 또는 GHCR에서 확인)
bash scripts/deploy/rollback.sh backend ghcr.io/portuna85/kraft/backend:<이전-sha>
bash scripts/deploy/rollback.sh web     ghcr.io/portuna85/kraft/web:<이전-sha>
```

---

## 스모크 테스트

배포 후 자동으로 아래 엔드포인트를 검사한다:

| 엔드포인트 | 예상 응답 |
|-----------|-----------|
| `GET /api/v1/rounds/latest` | 200 + `round` 필드 |
| `GET /api/v1/stats/frequency` | 200 |
| `GET /api/v1/stats/patterns` | 200 |
| `GET /api/v1/stats/companion` | 200 |
| `GET /admin/dashboard` | 302 (로그인 리다이렉트) |

실패 시 배포가 중단되고 롤백이 실행된다.

---

## GitHub Actions 페이지

https://github.com/portuna85/Kraft/actions
