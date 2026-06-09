ALTER TABLE lotto_fetch_logs
    ADD COLUMN api_client  VARCHAR(30) NULL,
    ADD COLUMN retry_count INT         NULL,
    ADD COLUMN latency_ms  BIGINT      NULL;
