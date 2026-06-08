ALTER TABLE lotto_fetch_logs ADD COLUMN IF NOT EXISTS api_client  VARCHAR(30) NULL;
ALTER TABLE lotto_fetch_logs ADD COLUMN IF NOT EXISTS retry_count INT         NULL;
ALTER TABLE lotto_fetch_logs ADD COLUMN IF NOT EXISTS latency_ms  BIGINT      NULL;
