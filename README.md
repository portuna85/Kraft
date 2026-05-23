<div align="center">

# 🎰 kLo

**`kraft-lotto`** — Spring Boot 4 기반 로또 서비스

[![Java](https://img.shields.io/badge/Java-25-007396?style=flat-square&logo=openjdk&logoColor=white)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)](#)
[![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white)](#)
[![Gradle](https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?style=flat-square&logo=gradle&logoColor=white)](#)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](#)

</div>

---

## 📑 목차

1. [기술 스택](#-기술-스택)
2. [프로젝트 구조](#-프로젝트-구조)
3. [주요 기능](#-주요-기능)
4. [사전 준비](#-사전-준비)
5. [로컬 실행](#-로컬-실행)
6. [테스트](#-테스트)
7. [Docker Compose](#-docker-compose)
8. [API 및 운영(Ops) 안내](#-api-및-운영ops-안내)
9. [인코딩 정책](#-인코딩-정책)

---

## 🧰 기술 스택

| 분류 | 사용 기술 |
|---|---|
| **언어 / 런타임** | Java 25 |
| **프레임워크** | Spring Boot 4.0.5 |
| **Spring 모듈** | Web · Thymeleaf · Validation · Data JPA · Flyway · Actuator · Cache |
| **데이터베이스** | MariaDB |
| **빌드 도구** | Gradle (Kotlin DSL) |

---

## 📂 프로젝트 구조

```
kLo/
├─ src/main/java/com/kraft/lotto/   # 애플리케이션 · 도메인 · 웹 · 서포트 계층
├─ src/main/resources/              # 설정 · Flyway 마이그레이션 · 템플릿 · 정적 리소스
├─ src/test/java/com/kraft/lotto/   # 단위 · 통합 테스트
├─ scripts/                         # 배포 및 유틸리티 스크립트
└─ docker-compose.yml               # 로컬 앱 + MariaDB 실행 환경
```

| 경로 | 역할 |
|---|---|
| `src/main/java/com/kraft/lotto` | 애플리케이션, 도메인, 웹, 서포트 코드 |
| `src/main/resources` | 애플리케이션 설정, Flyway 마이그레이션, Thymeleaf 템플릿, 정적 에셋 |
| `src/test/java/com/kraft/lotto` | 단위 및 통합 테스트 |
| `scripts/` | 배포 · 유틸리티 스크립트 |
| `docker-compose.yml` | 로컬 앱 + MariaDB 컨테이너 실행 환경 |

---

## ✨ 주요 기능

- 🎯 **당첨 번호 수집 / 스케줄러**
- 🔍 **당첨 번호 조회·목록 API 및 페이지**
- 📊 **통계 / 빈도 요약**
- 🤖 **규칙 기반 제약을 적용한 추천 번호 생성**
- 🛠 **운영(Ops) 엔드포인트 및 운영 페이지**

---

## ✅ 사전 준비

> [!NOTE]
> 실행 전에 아래 항목이 준비되어 있어야 합니다.

- **JDK 25**
- **Docker** *(선택)* — Compose 런타임 또는 컨테이너 기반 통합 경로를 사용할 경우

---

## 🚀 로컬 실행

### 1️⃣ 환경 변수 템플릿 복사

```bash
cp .env.example .env
```

### 2️⃣ 필수 값 채우기

`.env` 파일을 열어 필요한 설정값을 채워 넣습니다.

### 3️⃣ 애플리케이션 실행

```bash
./gradlew.bat bootRun
```

브라우저에서 아래 주소로 접속하세요 👇

```
http://localhost:8080
```

---

## 🧪 테스트

**기본 테스트**

```bash
./gradlew.bat test
```

**성능 스모크 테스트**

```bash
./gradlew.bat performanceSmokeTest
```

---

## 🐳 Docker Compose

앱과 MariaDB를 한 번에 빌드 후 백그라운드로 실행합니다.

```bash
docker compose up -d --build
```

---

## 🔌 API 및 운영(Ops) 안내

| 항목 | 내용 |
|---|---|
| **Actuator 노출 엔드포인트** | `health`, `info` |
| **OpenAPI UI** | `springdoc-openapi-starter-webmvc-ui` 의존성 포함 |
| **Ops 접근 제어** | `KRAFT_SECURITY_OPS_*` 설정으로 제어 |

> [!IMPORTANT]
> 운영(Ops) 엔드포인트는 IP 허용 목록과 토큰 기반 인증으로 보호되며,
> 프로덕션 환경에서는 **반드시** `KRAFT_SECURITY_OPS_*` 값을 적절히 설정해야 합니다.

---

## 🔤 인코딩 정책

> [!TIP]
> 저장소 내 모든 텍스트 파일은 **UTF-8** 로 작성합니다.

인코딩 검사가 필요하다면 아래 스크립트를 실행하세요.

```bash
python scripts/check_utf8.py
```

---

<div align="center">

**kLo** &nbsp;·&nbsp; Made with ☕ Java 25 &nbsp;·&nbsp; 🌱 Spring Boot 4

</div>
