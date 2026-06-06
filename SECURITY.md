# Security Policy

## 지원 범위

- 유지보수 기준 브랜치는 `main`이다.
- 오래된 태그나 파생 브랜치는 별도로 패치하지 않는다.

## 취약점 제보

보안 이슈는 공개 GitHub Issue로 올리지 않는다.

- 제보 메일: `portuna85@gmail.com`

제보 시 포함하면 좋은 정보:

- 취약점 유형
  - 예: 인증 우회, XSS, CSRF, SQL Injection, 정보 노출
- 재현 절차
- 영향 범위
  - 노출 가능한 데이터
  - 권한 상승 가능 여부
  - 서비스 중단 가능성
- 가능하면 PoC 또는 요청/응답 예시

원칙:

- 접수 확인은 가능한 빠르게 회신한다.
- 수정 전까지는 세부 내용을 공개하지 않는다.
- 수정 후 공개 여부는 제보자와 조율할 수 있다.

## 현재 보안 구조

### 공개 웹과 운영 경로 분리

- 공개 웹 경로는 일반 사용자 대상이다.
- `/ops/**`는 운영 API다.
- `/admin/login`, `/admin/ops/**`는 관리자 전용 경로다.

### 운영 API 보호

`/ops/**`는 `OpsAccessFilter`를 통해 보호된다.

- IP allowlist 검사
- `X-Ops-Token` 헤더 또는 `Authorization: Bearer ...` 토큰 검사
- 로그 마스킹

### 관리자 페이지 보호

`/admin/ops/**`는 Spring Security 인증이 필요하다.

- 로그인 페이지: `/admin/login`
- 기본 관리자 계정 ID: `admin`
- 비밀번호: `KRAFT_ADMIN_PASSWORD`
- 활성화 조건: `KRAFT_ADMIN_ENABLED=true`

### 프록시 레벨 차단

Caddy 설정 기준:

- 공개 도메인에서는 `/actuator*`, `/ops*`, `/admin*` 차단
- 관리자 도메인에서는 `/admin*`와 정적 자산만 허용

### 추가 방어

- 보안 헤더
- 레이트 리밋
- 관리자 페이지 `noindex`
- HSTS 옵션
- Actuator 제한 노출

## 운영 권장 사항

- `prod`에서는 `KRAFT_SECURITY_OPS_REQUIRED_TOKEN`을 반드시 설정한다.
- 운영 DB 계정은 최소 권한 원칙을 따른다.
- `KRAFT_ADMIN_PASSWORD`는 충분히 긴 임의 문자열을 사용한다.
- 관리자 기능이 필요 없으면 `KRAFT_ADMIN_ENABLED=false`를 유지한다.
- 공개 도메인에서 `/admin`과 `/ops` 경로를 직접 노출하지 않는다.
- 민감한 값은 `.env`, 시크릿 스토어, 배포 환경변수로만 관리한다.

## 범위 밖

아래 항목은 의도적으로 제공하지 않는다.

- 공개 SQL 콘솔
- 서버 쉘 실행 기능
- `.env` 열람 기능
- 임의 파일 편집 기능
- 운영 로그 전체 원본 다운로드 기능

## 테스트와 검증

보안 관련 점검 포인트:

- 비인증 사용자의 `/admin/ops` 접근 차단
- 비허용 IP의 `/ops/**` 접근 차단
- 잘못된 운영 토큰 거부
- 공개 도메인의 `/admin*`, `/ops*`, `/actuator*` 차단
- 관리자 페이지의 `noindex` 유지

코드와 운영 구조 변경 시 이 문서도 함께 갱신한다.
