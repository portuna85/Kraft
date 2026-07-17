-- idx_wn_draw_date(V8)는 어떤 리포지토리 쿼리도 draw_date로 필터/정렬하지 않아 사용되지 않는다.
-- 모든 조회는 round_no(유니크 키) 기준이다. 쓰기 비용만 발생하므로 제거한다.
-- 운영 DB의 flyway_schema_history가 이 마이그레이션을 계속 미적용(9)으로 관측해
-- 원인 불명 상태로 재시도되고 있어, 인덱스가 이미 없는 상태에서 재실행돼도
-- 실패하지 않도록 IF EXISTS로 방어한다(MariaDB 10.1.4+ 지원).
ALTER TABLE winning_numbers
    DROP INDEX IF EXISTS idx_wn_draw_date;
