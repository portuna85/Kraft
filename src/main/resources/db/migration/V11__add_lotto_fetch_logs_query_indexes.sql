-- Composite indexes for high-frequency ops queries on lotto_fetch_logs.
-- (status, fetched_at): failure-log list filtered by status ordered by time.
-- (drw_no, status): per-round status look-up used by failure-reason summaries.
create index idx_lotto_fetch_logs_status_fetched_at
    on lotto_fetch_logs (status, fetched_at);

create index idx_lotto_fetch_logs_drw_no_status
    on lotto_fetch_logs (drw_no, status);
