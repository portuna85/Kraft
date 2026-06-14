-- draw_date 내림차순 조회 최적화 (날짜 기반 필터·정렬 풀스캔 제거)
ALTER TABLE winning_numbers
    ADD INDEX idx_wn_draw_date (draw_date DESC);
