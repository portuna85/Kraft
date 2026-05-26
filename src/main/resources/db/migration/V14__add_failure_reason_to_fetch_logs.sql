ALTER TABLE lotto_fetch_logs
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(64) NULL AFTER message;

UPDATE lotto_fetch_logs
SET failure_reason = LOWER(TRIM(SUBSTRING(message, 8, LOCATE(';', message) - 8)))
WHERE status = 'FAILED'
  AND message LIKE 'reason=%;%'
  AND LOCATE(';', message) > 8
  AND (failure_reason IS NULL OR failure_reason = '');

CREATE INDEX IF NOT EXISTS idx_lotto_fetch_logs_status_reason_fetched_at
    ON lotto_fetch_logs (status, failure_reason, fetched_at);
