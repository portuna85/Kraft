CREATE TABLE pattern_stats_summary (
    stat_type             VARCHAR(20)  NOT NULL,
    bucket_key            INT          NOT NULL,
    draw_count            BIGINT       NOT NULL DEFAULT 0,
    total_draws           BIGINT       NOT NULL DEFAULT 0,
    last_calculated_round INT          NOT NULL DEFAULT 0,
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_pattern_stats_summary PRIMARY KEY (stat_type, bucket_key)
);
