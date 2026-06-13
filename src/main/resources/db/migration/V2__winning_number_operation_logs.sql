CREATE TABLE winning_number_operation_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_type VARCHAR(30) NOT NULL,
    execution_status VARCHAR(20) NOT NULL,
    round_no INT NULL,
    source_detail VARCHAR(255) NULL,
    message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_operation_logs_created (created_at DESC),
    KEY idx_operation_logs_round (round_no),
    KEY idx_operation_logs_type_status (operation_type, execution_status)
);
