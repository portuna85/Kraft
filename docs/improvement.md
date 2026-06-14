# KRAFT Lotto 코드 최적화·파인튜닝 결과

> 기준: 2026-06-14  
> 범위: 백엔드(Spring Boot) · 프론트엔드(Next.js) · 인프라(Docker · Caddy)  
> 분석: 3개 병렬 에이전트 전수 분석 → 버그 수정 · 성능 · 안정화 순 적용

---

## 1. 버그 수정 — ExternalWinningNumberPayloadMapper

**파일:** `src/main/java/com/kraft/winningnumber/ExternalWinningNumberPayloadMapper.java`

**문제:** `asInteger()` / `asLong()` 에서 문자열 파싱 실패 시 `NumberFormatException` 비처리  
→ 외부 로또 API가 예상치 못한 형식의 값을 반환하면 500 Internal Server Error 발생

**수정:** try-catch로 감싸 `ApiException(BAD_GATEWAY, "LOTTO_SOURCE_PARSE_ERROR")` 반환  
→ 외부 API 오류를 502로 정확히 표현, 스택 트레이스 노출 방지

```java
private Integer asInteger(Object value) {
    if (value == null) return null;
    if (value instanceof Number n) return n.intValue();
    try {
        return Integer.parseInt(value.toString().trim());
    } catch (NumberFormatException e) {
        throw new ApiException(HttpStatus.BAD_GATEWAY,
                "LOTTO_SOURCE_PARSE_ERROR", "숫자 변환 실패: " + value);
    }
}
```

---

## 2. 버그 수정 — LottoRecommendationService (Fisher-Yates)

**파일:** `src/main/java/com/kraft/recommend/LottoRecommendationService.java`

**문제:** `generateOne()` 에서 while 루프 사용  
→ 제외 번호 39개일 때 평균 ~45회 반복, 확률적 효율 저하

**수정:** Fisher-Yates 셔플로 교체 → O(n) 보장, 루프 횟수 고정

```java
private List<Integer> generateOne(Set<Integer> excluded) {
    List<Integer> candidates = new ArrayList<>(45 - excluded.size());
    for (int i = 1; i <= 45; i++) {
        if (!excluded.contains(i)) candidates.add(i);
    }
    for (int i = candidates.size() - 1; i > 0; i--) {
        int j = random.nextInt(i + 1);
        int tmp = candidates.get(i); candidates.set(i, candidates.get(j)); candidates.set(j, tmp);
    }
    return lottoNumberCodec.normalize(candidates.subList(0, 6));
}
```

---

## 3. 렌더링 최적화 — latest/page.tsx

**파일:** `web/src/app/latest/page.tsx`

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 렌더링 전략 | `force-dynamic` (매 요청 SSR) | `revalidate = 60` (ISR) |
| 백엔드 호출 수 | 2회 (generateMetadata + Page) | 1회 (`React.cache()` 중복 제거) |

**이유:**  
- `RevalidateWebhookListener`가 신규 회차 수집 즉시 캐시를 무효화하므로 `force-dynamic` 불필요  
- `React.cache()`는 동일 요청 범위 내에서 함수 결과를 메모이제이션

```tsx
import { cache } from "react";
export const revalidate = 60;
const getCachedLatest = cache(getLatestWinningNumber);
```

---

## 4. 렌더링 최적화 — rounds/page.tsx

**파일:** `web/src/app/rounds/page.tsx`

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 렌더링 전략 | `force-dynamic` | `revalidate = 300` |

**이유:** 회차 목록은 신규 회차 수집 시에만 변경됨. 페이지네이션 파라미터 조합별로 5분 캐시하면  
동일 URL 반복 방문 시 SSR 생략 가능

---

## 5. DB 인덱스 추가 — V8__indexes.sql

**파일:** `src/main/resources/db/migration/V8__indexes.sql`

**문제:** `winning_numbers.draw_date` 컬럼에 인덱스 없음 → 날짜 기반 조회·정렬 시 풀스캔

```sql
ALTER TABLE winning_numbers
    ADD INDEX idx_wn_draw_date (draw_date DESC);
```

---

## 6. HikariCP 튜닝

**파일:** `application.yml` / `application-prod.yml`

| 설정 | 로컬 (application.yml) | 프로덕션 (application-prod.yml) |
|------|----------------------|-------------------------------|
| `maximum-pool-size` | 5 (명시) | 10 → **15** |
| `minimum-idle` | 1 (명시) | 2 → **3** |

**이유:** 기존 base application.yml에 pool size 미설정으로 기본값이 암묵적으로 적용되던 문제 해소.  
프로덕션 풀 사이즈를 서버 vCPU × 4 기준으로 상향.

---

## 7. Hibernate 배치 처리 설정

**파일:** `src/main/resources/application-prod.yml`

```yaml
jpa:
  properties:
    hibernate:
      jdbc:
        batch_size: 25
        fetch_size: 50
      order_inserts: true
      order_updates: true
```

**이유:** `StatisticsSummaryRebuilder`의 통계 재빌드 시 대량 INSERT/UPDATE가 배치로 처리되어  
DB 왕복 횟수 감소 (최대 1/25로 단축)

---

## 8. Dockerfile 레이어 최적화

**파일:** `Dockerfile`

**변경 전:** `COPY src` → `./gradlew bootJar` → 소스 수정마다 Gradle 의존성 재다운로드  
**변경 후:** 의존성 다운로드 레이어 분리 → `build.gradle.kts` 변경 시에만 재다운로드

```dockerfile
# 의존성 캐시 (build.gradle.kts 불변 시 캐시 히트)
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.lockfile ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test
```

**추가:** JVM 컨테이너 친화적 옵션

```dockerfile
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
```

- `UseZGC`: 저지연 GC (로또 서비스 특성상 짧은 Pause 중요)
- `MaxRAMPercentage=75.0`: 컨테이너 메모리 제한의 75%를 힙으로 자동 설정
- `ExitOnOutOfMemoryError`: OOM 시 컨테이너 즉시 종료 → Docker restart 정책으로 자가 복구

---

## 9. docker-compose.yml 리소스 제한 + 로깅

**파일:** `docker-compose.yml`

**변경 사항:**

| 서비스 | 추가 내용 |
|--------|----------|
| `backend` | `memory: 1g / cpus: 1.0` 제한, `reservation: 512m`, 로그 드라이버 |
| `mariadb` | `--innodb-buffer-pool-size=512M`, `memory: 1g` 제한, 로그 드라이버 |
| `web` | 로그 드라이버 |

**로그 드라이버 (공통):**
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "50m"
    max-file: "3"
```

**이유:** 제한 없을 경우 메모리 폭주 → OOM Killer 개입 → 예측 불가 종료 위험

---

## 10. Caddyfile — brotli + 보안 헤더 + 정적 캐시

**파일:** `caddy/Caddyfile`

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 압축 | `encode zstd gzip` | `encode brotli zstd gzip` |
| 보안 헤더 | HSTS만 | + `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy` |
| 정적 자산 캐시 | 없음 | `/_next/static/*` → `max-age=31536000, immutable` |

**이유:**
- `brotli`: 최신 브라우저에서 gzip 대비 15~25% 추가 압축
- `X-Content-Type-Options: nosniff`: MIME 스니핑 방지
- `immutable` 캐시: Next.js 콘텐츠 해시가 포함된 정적 파일은 1년 캐시 안전
