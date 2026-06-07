CREATE TABLE companion_pair_summary (
    ball                  INT          NOT NULL,
    other_ball            INT          NOT NULL,
    hit_count             BIGINT       NOT NULL DEFAULT 0,
    last_calculated_round INT          NOT NULL DEFAULT 0,
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_companion_pair_summary PRIMARY KEY (ball, other_ball)
);
