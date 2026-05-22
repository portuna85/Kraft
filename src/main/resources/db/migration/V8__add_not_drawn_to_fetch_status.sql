ALTER TABLE lotto_fetch_logs
    DROP CONSTRAINT chk_lfl_status;

ALTER TABLE lotto_fetch_logs
    ADD CONSTRAINT chk_lfl_status CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED', 'NOT_DRAWN'));
