# Admin Console 개선 정리

이 문서는 관리자 기능의 현재 상태와 앞으로의 개선 방향을 분리해서 정리한다.

작성 기준일: 2026-06-06

## 목적

현재 운영 중인 로또 수집, 뉴스 검수, 실패 로그 확인 기능을 더 안전하고 일관되게 관리할 수 있도록 Admin Console의 목표 상태를 정의한다.

중요한 전제:

- Admin Console은 제한된 운영 도구여야 한다.
- Admin Console은 서버 쉘이 아니다.
- Admin Console은 임의 SQL 콘솔이 아니다.
- Admin Console은 비밀값 열람 도구가 아니다.

## 1. 현재 상태

### 이미 구현된 것

현재 코드 기준으로 아래 기능은 존재한다.

- 관리자 로그인 페이지 `/admin/login`
- 관리자 보호 경로 `/admin/ops/**`
- 수집 실패 대시보드 `/admin/ops`
- 수집 실행 페이지 `/admin/ops/collection`
- 뉴스 승인/거부/차단 페이지 `/admin/ops/news`
- 감사 로그 페이지 `/admin/ops/audit`
- 운영 API `/ops/**`
- 운영 API용 IP allowlist와 토큰 인증
- 공개 도메인에서 `/admin*`, `/ops*`, `/actuator*` 차단
- 관리자 로그인 페이지 `noindex`
- Prometheus, Grafana, Alertmanager, Caddy 구성

### 현재 구현상의 제한

현재 상태는 최소한의 관리자 기능에 가깝다.

주요 한계:

- 관리자 인증은 고정 ID `admin` + 비밀번호 방식이다.
- 역할 분리와 세분화된 권한 모델이 없다.
- 관리자 도메인 전용 진입 강제가 애플리케이션 레벨에서 완전히 닫혀 있지는 않다.
- 운영 API와 관리자 UI가 기능상 분리되어 있지만 운영 절차 문서가 자주 어긋날 수 있다.
- 일부 관리자 컨트롤러 코드는 과거 OAuth2 흔적을 아직 가지고 있다.

즉, 현재는 운영 가능한 수준이지만 장기적으로는 인증, 권한, 감사 체계를 더 명확히 정리해야 한다.

## 2. 현재 구조 요약

```text
공개 사용자
  -> 공개 도메인
  -> 공개 페이지

운영자
  -> 관리자 도메인 또는 SSH 터널
  -> /admin/login
  -> /admin/ops/**

운영 자동화/운영자
  -> /ops/**
  -> IP allowlist + ops token
```

## 3. 목표 상태

### 3.1 공개 영역과 관리자 영역의 명확한 분리

공개 도메인:

```text
https://www.kraft.io.kr
```

허용 예시:

- `/`
- `/latest`
- `/rounds`
- `/frequency`
- `/stats`
- `/analysis`
- `/companion`
- `/news`
- `/methodology`
- `/data-source`
- `/faq`
- `/responsible-play`
- `/privacy`
- `/terms`
- `/contact`

차단 예시:

- `/admin*`
- `/ops*`
- `/actuator*`

관리자 도메인:

```text
https://admin.kraft.io.kr
```

허용 예시:

- `/admin/login`
- `/admin/ops`
- `/admin/ops/collection`
- `/admin/ops/news`
- `/admin/ops/audit`

불허 예시:

- `/ops*`
- `/actuator*`
- 임의 파일 조회
- 임의 명령 실행

### 3.2 인증 계층 강화

권장 순서:

1. 프록시 또는 Access Provider 레벨 진입 제어
2. 애플리케이션 로그인
3. 역할/권한 검사
4. 감사 로그

현실적인 대안:

- 단기: 현재 폼 로그인 유지 + 관리자 도메인 강제 + 감사 로그 강화
- 중기: Google OAuth 또는 Access Provider 연동
- 장기: 권한 단위 세분화와 고위험 작업 재인증

## 4. 권장 개선 항목

### P0. 현재 운영 안정화

- 관리자 도메인과 공개 도메인 역할을 문서와 설정에서 일치시킨다.
- `KRAFT_ADMIN_ENABLED`, `KRAFT_ADMIN_PASSWORD` 운영 점검 절차를 표준화한다.
- 관리자 경로 접속 절차를 SSH 터널 방식과 관리자 도메인 방식으로 분리 문서화한다.
- 감사 로그에서 누가 어떤 작업을 실행했는지 식별 가능한 필드를 일관되게 남긴다.

### P1. 인증/권한 정리

- 관리자 사용자 식별자를 `admin` 단일 계정에서 운영자 개별 계정으로 전환
- `VIEW`, `COLLECTION_WRITE`, `NEWS_WRITE`, `AUDIT_READ` 등 권한 분리
- 고위험 작업에 확인 문구 또는 재인증 도입

예시 권한 모델:

```text
ROLE_ADMIN_VIEWER
ROLE_ADMIN_OPERATOR
ROLE_ADMIN_NEWS_MANAGER
ROLE_ADMIN_AUDITOR
ROLE_ADMIN_SUPER_ADMIN
```

### P2. 운영 화면 확장

현재 구현된 화면 외에 아래가 후보가 될 수 있다.

- 시스템 상태 화면
- 캐시 관리 화면
- SEO 점검 화면
- 배포 버전/빌드 정보 화면
- 뉴스 수집 상태/대기열 화면

## 5. 관리자 화면별 목표

### 5.1 대시보드 `/admin/ops`

현재:

- 수집 실패 사유와 최근 실패 목록 중심

강화 후보:

- 최신 저장 회차
- 예상 최신 회차
- 마지막 성공 수집 시각
- 판매점 수집 상태
- 빌드 커밋
- 활성 프로필

### 5.2 수집 관리 `/admin/ops/collection`

현재:

- 최신 회차 수집
- 특정 회차 판매점 수집

강화 후보:

- 특정 회차 강제 재수집
- 실패 회차 재시도
- 최근 실행 이력
- 중복 실행 방지 상태 표시

### 5.3 뉴스 관리 `/admin/ops/news`

현재:

- 승인
- 거부
- 도메인 차단
- 키워드 차단

강화 후보:

- 승인 대기/승인/거부 탭 분리
- 대량 처리
- 차단 규칙 검색
- 차단 사유 표준화

### 5.4 감사 로그 `/admin/ops/audit`

현재:

- 감사 로그 페이지 존재

강화 후보:

- 작업 유형별 필터
- 기간 필터
- 결과별 필터
- 실패 원인 마스킹과 표준 메시지

## 6. 보안 요구사항

### 변경 작업은 POST만 사용

허용 방향:

- `POST /admin/ops/collection/latest`
- `POST /admin/ops/collection/stores`
- `POST /admin/ops/news/{id}/approve`
- `POST /admin/ops/news/{id}/reject`

금지 방향:

- GET 요청으로 상태 변경

### CSRF 유지

관리자 화면의 상태 변경 폼은 CSRF를 유지해야 한다.

### 비밀값 노출 금지

관리자 화면에서 보여주지 않아야 하는 것:

- DB 비밀번호
- 운영 토큰 원문
- OAuth client secret
- `.env` 전체 덤프
- 시스템 프로퍼티 전체 덤프

### 감사 로그 일관성

모든 변경 작업은 최소한 아래를 남겨야 한다.

- actor
- action
- target
- request_ip
- user_agent
- result
- created_at

## 7. 프록시 정책 방향

공개 도메인:

```text
/admin*    -> 차단
/ops*      -> 차단
/actuator* -> 차단
```

관리자 도메인:

```text
/admin*와 관리자 정적 자산만 허용
/ops* 직접 접근은 차단
/actuator* 직접 접근은 차단
```

이 원칙은 현재 `docker/caddy/Caddyfile` 방향과 일치한다.

## 8. 데이터 모델 후보

현재 테이블:

- `admin_audit_log`
- `news_blocked_domain`
- `news_blocked_keyword`

추가 후보:

- `admin_task_execution`
  - 관리자 작업 실행 상태 추적

추가가 필요한 경우에만 도입한다. 현재 기능만으로는 필수는 아니다.

## 9. 테스트 기준

### 인증

- 비로그인 사용자는 `/admin/ops` 접근 불가
- 관리자 로그인 성공 시 `/admin/ops` 이동
- 로그인 실패 시 `/admin/login?error` 이동

### 운영 API

- 허용되지 않은 IP는 `/ops/**` 접근 불가
- 토큰 미제공 또는 오토큰은 거부

### 프록시

- 공개 도메인에서 `/admin*`, `/ops*`, `/actuator*` 차단
- 관리자 도메인에서 비관리자 경로 차단

### 감사 로그

- 상태 변경 작업 후 감사 로그 생성
- 실패 작업도 감사 로그 생성

## 10. 권장 실행 순서

현실적인 개선 순서는 아래와 같다.

1. 현재 문서와 운영 절차를 코드에 맞춘다.
2. 관리자 도메인 진입 정책을 명확히 고정한다.
3. 관리자 계정 모델과 권한 모델을 분리한다.
4. 수집, 뉴스, 감사 화면의 실행 이력과 에러 표시를 강화한다.
5. 필요할 때만 시스템, 캐시, SEO 화면을 추가한다.

## 결론

현재 Admin Console은 이미 운영에 필요한 최소 기능은 제공한다. 다만 장기 운영 관점에서는 인증 방식, 권한 모델, 감사 로그 품질, 관리자 도메인 정책을 더 엄격하게 정리해야 한다.

즉, 다음 단계의 핵심은 기능 추가보다 먼저 아래 네 가지다.

1. 접속 경로 정리
2. 인증 정리
3. 권한 정리
4. 감사 정리
