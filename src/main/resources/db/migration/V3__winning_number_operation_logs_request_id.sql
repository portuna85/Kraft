ALTER TABLE winning_number_operation_logs
    ADD COLUMN request_id VARCHAR(100) NULL AFTER message;

CREATE INDEX idx_operation_logs_request_id ON winning_number_operation_logs (request_id);
