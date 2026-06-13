CREATE TABLE winning_number_frequency_summary (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    ball_number  INT    NOT NULL,
    frequency    INT    NOT NULL DEFAULT 0,
    last_round   INT    NOT NULL DEFAULT 0,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_frequency_ball (ball_number)
);

CREATE TABLE pattern_stats_summary (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    stat_type   VARCHAR(50)  NOT NULL,
    bucket_key  VARCHAR(100) NOT NULL,
    count_val   INT          NOT NULL DEFAULT 0,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pattern_type_bucket (stat_type, bucket_key)
);

CREATE TABLE companion_pair_summary (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    ball_a       INT    NOT NULL,
    ball_b       INT    NOT NULL,
    co_count     INT    NOT NULL DEFAULT 0,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_companion_pair (ball_a, ball_b),
    CONSTRAINT chk_companion_order CHECK (ball_a < ball_b)
);
