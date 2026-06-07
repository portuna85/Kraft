-- (fetched_at) ⊂ (fetched_at, id) 이므로 단독 인덱스는 쓰기 증폭만 유발한다.
-- (drw_no)     ⊂ (drw_no, status) 이므로 동일하게 제거한다.
DROP INDEX idx_lotto_fetch_logs_fetched_at ON lotto_fetch_logs;
DROP INDEX idx_lotto_fetch_logs_drw_no     ON lotto_fetch_logs;
