ALTER TABLE saved_number_client_locks
    ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ADD COLUMN last_used_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

CREATE INDEX idx_saved_number_client_locks_last_used_at ON saved_number_client_locks (last_used_at);
