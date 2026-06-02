-- ============================================================
-- Baseline schema: consolidated from V1–V18
-- ============================================================

CREATE TABLE winning_numbers (
    round                INT          NOT NULL,
    draw_date            DATE         NOT NULL,
    n1                   INT          NOT NULL,
    n2                   INT          NOT NULL,
    n3                   INT          NOT NULL,
    n4                   INT          NOT NULL,
    n5                   INT          NOT NULL,
    n6                   INT          NOT NULL,
    bonus_number         INT          NOT NULL,
    first_prize          BIGINT       NOT NULL,
    first_winners        INT          NOT NULL,
    total_sales          BIGINT       NOT NULL,
    created_at           DATETIME(6)  NOT NULL,
    first_accum_amount   BIGINT       NOT NULL DEFAULT 0,
    raw_json             LONGTEXT     NULL,
    fetched_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version              BIGINT       NOT NULL DEFAULT 0,
    second_prize         BIGINT       NOT NULL DEFAULT 0,
    second_winners       INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_winning_numbers               PRIMARY KEY (round),
    CONSTRAINT chk_wn_round_positive            CHECK (round > 0),
    CONSTRAINT chk_wn_n1_range                  CHECK (n1 BETWEEN 1 AND 45),
    CONSTRAINT chk_wn_n2_range                  CHECK (n2 BETWEEN 1 AND 45),
    CONSTRAINT chk_wn_n3_range                  CHECK (n3 BETWEEN 1 AND 45),
    CONSTRAINT chk_wn_n4_range                  CHECK (n4 BETWEEN 1 AND 45),
    CONSTRAINT chk_wn_n5_range                  CHECK (n5 BETWEEN 1 AND 45),
    CONSTRAINT chk_wn_n6_range                  CHECK (n6 BETWEEN 1 AND 45),
    CONSTRAINT chk_wn_bonus_range               CHECK (bonus_number BETWEEN 1 AND 45),
    CONSTRAINT chk_wn_n1_lt_n2                  CHECK (n1 < n2),
    CONSTRAINT chk_wn_n2_lt_n3                  CHECK (n2 < n3),
    CONSTRAINT chk_wn_n3_lt_n4                  CHECK (n3 < n4),
    CONSTRAINT chk_wn_n4_lt_n5                  CHECK (n4 < n5),
    CONSTRAINT chk_wn_n5_lt_n6                  CHECK (n5 < n6),
    CONSTRAINT chk_wn_bonus_distinct            CHECK (bonus_number NOT IN (n1, n2, n3, n4, n5, n6)),
    CONSTRAINT chk_wn_first_prize_nonneg        CHECK (first_prize >= 0),
    CONSTRAINT chk_wn_first_winners_nonneg      CHECK (first_winners >= 0),
    CONSTRAINT chk_wn_total_sales_nonneg        CHECK (total_sales >= 0),
    CONSTRAINT chk_wn_first_accum_nonneg        CHECK (first_accum_amount >= 0),
    CONSTRAINT chk_wn_second_prize_nonneg       CHECK (second_prize >= 0),
    CONSTRAINT chk_wn_second_winners_nonneg     CHECK (second_winners >= 0)
);

CREATE INDEX idx_winning_numbers_draw_date  ON winning_numbers (draw_date);
CREATE INDEX idx_winning_numbers_version    ON winning_numbers (version);
CREATE INDEX idx_winning_numbers_fetched_at ON winning_numbers (fetched_at);

-- ============================================================

CREATE TABLE lotto_fetch_logs (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    drw_no         INT           NOT NULL,
    status         VARCHAR(20)   NOT NULL,
    message        VARCHAR(500)  NULL,
    failure_reason VARCHAR(64)   NULL,
    response_code  INT           NULL,
    raw_response   VARCHAR(4000) NULL,
    fetched_at     DATETIME(6)   NOT NULL,
    winning_round  INT           NULL,
    CONSTRAINT pk_lotto_fetch_logs        PRIMARY KEY (id),
    CONSTRAINT chk_lfl_drw_no_positive   CHECK (drw_no > 0),
    CONSTRAINT chk_lfl_status            CHECK (status IN ('SUCCESS', 'FAILED', 'SKIPPED', 'NOT_DRAWN')),
    CONSTRAINT fk_lotto_fetch_logs_winning_round
        FOREIGN KEY (winning_round) REFERENCES winning_numbers (round) ON DELETE SET NULL
);

CREATE INDEX idx_lotto_fetch_logs_drw_no                    ON lotto_fetch_logs (drw_no);
CREATE INDEX idx_lotto_fetch_logs_fetched_at                ON lotto_fetch_logs (fetched_at);
CREATE INDEX ix_lotto_fetch_logs_fetched_at_id              ON lotto_fetch_logs (fetched_at, id);
CREATE INDEX idx_lotto_fetch_logs_winning_round             ON lotto_fetch_logs (winning_round);
CREATE INDEX idx_lotto_fetch_logs_status_fetched_at         ON lotto_fetch_logs (status, fetched_at);
CREATE INDEX idx_lotto_fetch_logs_drw_no_status             ON lotto_fetch_logs (drw_no, status);
CREATE INDEX idx_lotto_fetch_logs_status_reason_fetched_at  ON lotto_fetch_logs (status, failure_reason, fetched_at);

-- ============================================================

CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);

-- ============================================================

CREATE TABLE winning_number_frequency_summary (
    ball                  INT          NOT NULL,
    hit_count             BIGINT       NOT NULL DEFAULT 0,
    last_calculated_round INT          NOT NULL DEFAULT 0,
    updated_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_winning_number_frequency_summary   PRIMARY KEY (ball),
    CONSTRAINT ck_wnfs_ball                          CHECK (ball BETWEEN 1 AND 45),
    CONSTRAINT ck_wnfs_count                         CHECK (hit_count >= 0),
    CONSTRAINT ck_wnfs_round                         CHECK (last_calculated_round >= 0)
);

-- ============================================================

CREATE TABLE news_articles (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    title        VARCHAR(500)  NOT NULL,
    link         VARCHAR(2000) NOT NULL,
    link_hash    VARCHAR(64)   NOT NULL,
    description  TEXT,
    source       VARCHAR(200),
    pub_date     DATETIME,
    collected_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_news_link_hash (link_hash),
    INDEX idx_news_pub_date     (pub_date),
    INDEX idx_news_collected_at (collected_at)
);

-- ============================================================

CREATE TABLE winning_stores (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    round     INT          NOT NULL,
    grade     INT          NOT NULL,
    name      VARCHAR(200) NOT NULL,
    address   VARCHAR(300),
    win_count INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_winning_stores_round_grade (round, grade)
);
