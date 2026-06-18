# Kraft Lotto

로또 6/45 당첨 결과 조회, 번호 추천, 통계, 번호 저장 기능을 제공하는 서비스.

## 기술 스택

| 계층 | 기술 |
|------|------|
| 프론트엔드 | Next.js (App Router), TypeScript |
| 백엔드 | Spring Boot, Java 21+ |
| DB | MariaDB (운영) / H2 (로컬 기본) |
| 리버스 프록시 | Caddy |
| CI/CD | GitHub Actions → GHCR → 서버 배포 |

## 프로젝트 구조

```
Kraft/
├── src/                    # Spring Boot 백엔드 (포트 8080)
│   └── main/java/com/kraft/
│       ├── recommend/      # 번호 추천
│       ├── winningnumber/  # 당첨번호 수집·조회
│       └── admin/          # 관리자 (Thymeleaf SSR)
├── web/                    # Next.js 프론트엔드 (포트 3000)
└── docker-compose.yml      # 프로덕션/Docker 환경
```

## 로컬 개발

### 사전 조건
JDK 21+, Node.js 24+, Docker Desktop

### 빠른 핫리로드 (DB만 Docker)

```bash
docker compose -f docker-compose.dev.yml up -d
./gradlew bootRun --args='--spring.profiles.active=local'
cd web && npm run dev
```

`.env.local`에서 MariaDB 연결 정보를 설정해야 합니다 (`.env.local.example` 참고).

### 접속

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080 |
| 관리자 | http://localhost:8080/admin |

관리자 페이지 접속 상세 가이드는 `docs/`에 별도 작성되어 있습니다 (로컬 전용 문서, 저장소에 커밋되지 않음).

## 배포

`main` 브랜치 push 시 GitHub Actions가 빌드·테스트 → Docker 이미지 빌드 → GHCR 업로드 → 서버 무중단 배포를 자동 수행합니다.
