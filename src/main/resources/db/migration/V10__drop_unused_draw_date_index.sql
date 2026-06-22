-- idx_wn_draw_date(V8)는 어떤 리포지토리 쿼리도 draw_date로 필터/정렬하지 않아 사용되지 않는다.
-- 모든 조회는 round_no(유니크 키) 기준이다. 쓰기 비용만 발생하므로 제거한다.
ALTER TABLE winning_numbers
    DROP INDEX idx_wn_draw_date;
