CREATE TABLE winning_numbers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_no INT NOT NULL,
    draw_date DATE NOT NULL,
    n1 INT NOT NULL,
    n2 INT NOT NULL,
    n3 INT NOT NULL,
    n4 INT NOT NULL,
    n5 INT NOT NULL,
    n6 INT NOT NULL,
    bonus_number INT NOT NULL,
    first_prize_amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_winning_numbers_round (round_no),
    CONSTRAINT chk_winning_numbers_range CHECK (
        n1 BETWEEN 1 AND 45 AND
        n2 BETWEEN 1 AND 45 AND
        n3 BETWEEN 1 AND 45 AND
        n4 BETWEEN 1 AND 45 AND
        n5 BETWEEN 1 AND 45 AND
        n6 BETWEEN 1 AND 45 AND
        bonus_number BETWEEN 1 AND 45
    ),
    CONSTRAINT chk_winning_numbers_sorted CHECK (n1 < n2 AND n2 < n3 AND n3 < n4 AND n4 < n5 AND n5 < n6),
    CONSTRAINT chk_winning_numbers_bonus CHECK (
        bonus_number <> n1 AND
        bonus_number <> n2 AND
        bonus_number <> n3 AND
        bonus_number <> n4 AND
        bonus_number <> n5 AND
        bonus_number <> n6
    )
);

CREATE TABLE saved_numbers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    client_token_hash CHAR(64) NOT NULL,
    numbers VARCHAR(32) NOT NULL,
    label VARCHAR(100) NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saved_client_numbers (client_token_hash, numbers),
    KEY idx_saved_client_created (client_token_hash, created_at DESC)
);
