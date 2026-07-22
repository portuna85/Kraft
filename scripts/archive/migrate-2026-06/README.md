# migrate-2026-06 (archived)

2026-06 옛 시스템 → KRAFT 데이터 이전용 1회성 도구. **이전은 이미 완료됨.**

이 디렉터리는 참고용으로만 남겨두며, 그대로 재실행하지 말 것. 실제 실행 시 다음과 같은
알려진 버그가 있다:

- `03-transform-saved-numbers.sh`: `OLD_SAVED_TOKEN_COL`/`OLD_SAVED_DATE_COL`을 선언만 하고
  실제 SQL rewrite에 쓰지 않는다. staging 테이블은 신 스키마(`client_token_hash`,
  `created_at`)로 만들어지는데 옛 덤프의 INSERT문은 컬럼명 재작성 없이 그대로 들어가므로
  `device_token`/`saved_at` 컬럼 불일치로 실패할 수 있다.
- `02-import-winning-numbers.sh`: FK/unique 체크를 끄는 세션과 실제 벌크 로드를 하는 세션이
  분리돼 있어, session variable(`foreign_key_checks=0`)이 로드에 적용되지 않는다.
- `04-rebuild-stats.sh`: `KRAFT_OPS_TOKEN`을 필수로 요구하지만 실제로는 어디에도 쓰이지
  않고, 공개 GET 엔드포인트의 side effect에만 의존한다.
- `05-validate.sh`: summary가 비어 있어도 WARN만 내고 전체 검증을 성공 처리한다. saved
  count 관련 주석과 실제 허용 조건도 다르다.

재사용이 필요해지면 위 버그를 먼저 고치고, old/new DB fixture로 01~06을 end-to-end 실행하는
CI 검증을 추가한 뒤 실행할 것.
