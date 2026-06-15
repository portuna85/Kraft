# Kraft Lotto — 개발 가이드

## 로컬 개발 환경 (IntelliJ + localhost)

### 사전 조건
- JDK 21+
- Node.js 24+
- IntelliJ IDEA (Spring Boot 플러그인 포함)

### 백엔드 실행 (Spring Boot — localhost:8080)

IntelliJ 실행 구성 **"Kraft Backend"** 를 사용한다.
- 메인 클래스: `com.kraft.Application`
- 프로파일: `local` (자동 적용됨)
- DB: H2 인메모리 (Docker 불필요)
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:kraft-lotto`)

MariaDB(Docker)로 전환하려면 프로젝트 루트에 `.env.local` 파일을 생성한다:
```properties
KRAFT_DB_URL=jdbc:mariadb://localhost:3306/kraft_lotto?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
KRAFT_DB_USERNAME=kraft_lotto
KRAFT_DB_PASSWORD=<비밀번호>
KRAFT_FLYWAY_ENABLED=true
KRAFT_JPA_DDL_AUTO=validate
```

### 프론트엔드 실행 (Next.js — localhost:3000)

IntelliJ 실행 구성 **"Kraft Frontend"** 를 사용하거나 터미널에서:
```bash
cd web
npm run dev
```

`web/.env.local` 이 이미 올바르게 설정되어 있다:
```
KRAFT_BACKEND_INTERNAL_URL=http://localhost:8080
KRAFT_PUBLIC_BASE_URL=http://localhost:3000
```

### 동시 실행

IntelliJ 실행 구성 **"Kraft Local (Backend + Frontend)"** 로 백엔드와 프론트엔드를 한 번에 시작한다.

### 접속 URL

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080 |
| H2 콘솔 | http://localhost:8080/h2-console |
| 관리자 | http://localhost:8080/admin |

---

## 프로젝트 구조

```
Kraft/
├── src/                    # Spring Boot 백엔드 (포트 8080)
│   └── main/java/com/kraft/
│       ├── recommend/      # 번호 추천 (CombinationScorer, LottoRecommendationService)
│       ├── winningnumber/  # 당첨번호 수집·조회
│       └── admin/          # 관리자 (Thymeleaf SSR)
├── web/                    # Next.js 프론트엔드 (포트 3000)
│   └── src/
│       ├── app/            # App Router 페이지
│       └── components/     # 공통 컴포넌트
└── docker-compose.yml      # 프로덕션/Docker 환경
```

## 핵심 기능

- **번호 추천**: 역대 1등 조합 자동 제외, 당첨금 최대화 모드 (비인기 조합 우선)
- **통계**: 빈도·패턴·동반 출현 분석
- **저장**: 디바이스 토큰 기반 조합 저장
- **관리자**: Spring Security + Thymeleaf SSR (로그인·회차관리·감사로그)
