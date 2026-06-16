# 로컬 개발 환경

코드 변경 → 브라우저에서 즉시 확인. Docker 재빌드 없음.

## 사전 조건

| 도구 | 버전 | 확인 |
|------|------|------|
| JDK | 25 (Temurin) | `java -version` |
| Node.js | 24+ | `node -v` |
| Docker Desktop | 최신 | `docker --version` |

---

## 방법 A — H2 인메모리 DB (가장 빠름, Docker 불필요)

### 1. 백엔드 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

또는 IntelliJ 실행 구성 **"Kraft Backend"** 사용.

### 2. 프론트엔드 실행

```bash
cd web && npm run dev
```

### 접속 URL

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080 |
| H2 콘솔 | http://localhost:8080/h2-console |
| 관리자 | http://localhost:8080/admin |

> H2 콘솔 JDBC URL: `jdbc:h2:mem:kraft-lotto`  
> 매 실행마다 DB가 초기화됨 (테스트 데이터 필요 시 재입력)

---

## 방법 B — MariaDB (실 DB 환경, 핫리로드)

데이터가 재시작 후에도 유지되어야 할 때 사용.

### 1. DB 컨테이너 시작

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 2. `.env.local` 설정 (최초 1회)

```bash
cp .env.local.example .env.local
vim .env.local
```

아래 MariaDB 섹션 주석 해제:

```properties
KRAFT_DB_URL=jdbc:mariadb://localhost:3306/kraft_lotto?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
KRAFT_DB_USERNAME=kraft_lotto
KRAFT_DB_PASSWORD=devpass
KRAFT_FLYWAY_ENABLED=true
KRAFT_FLYWAY_BASELINE_ON_MIGRATE=true
KRAFT_FLYWAY_BASELINE_VERSION=7
KRAFT_JPA_DDL_AUTO=validate
```

> `devpass` 는 `docker-compose.dev.yml` 의 기본 비밀번호.

### 3. 백엔드 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. 프론트엔드 실행

```bash
cd web && npm run dev
```

---

## 핫리로드 동작 방식

| 계층 | 트리거 | 결과 |
|------|--------|------|
| 백엔드 | IntelliJ Build (`Ctrl+F9`) 또는 `./gradlew compileJava` | Spring Boot DevTools가 자동 재시작 (~2초) |
| 프론트엔드 | 파일 저장 | Next.js HMR 즉시 반영 |
| Thymeleaf 템플릿 | 파일 저장 | DevTools 캐시 무효화로 재시작 없이 반영 |

---

## 테스트 실행

```bash
# 전체 테스트 (백엔드)
./gradlew test

# 커버리지 포함
./gradlew test -PstrictCoverage=true

# 프론트엔드
cd web && npm test

# 정적 분석
./gradlew check -PstrictStatic=true -x test
```

---

## 관리자 계정

로컬 실행 시 `.env.local` 의 아래 값으로 자동 생성됨:

```properties
KRAFT_ADMIN_BOOTSTRAP_USERNAME=admin
KRAFT_ADMIN_BOOTSTRAP_PASSWORD=admin
```

http://localhost:8080/admin 에서 로그인.
