-- raw_response is truncated to 4000 chars in Java before persistence; align DB column type accordingly
ALTER TABLE lotto_fetch_logs
    MODIFY COLUMN raw_response VARCHAR(4000) NULL;
