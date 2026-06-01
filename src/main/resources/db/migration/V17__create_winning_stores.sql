CREATE TABLE winning_stores (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    round      INT          NOT NULL,
    grade      INT          NOT NULL,
    name       VARCHAR(200) NOT NULL,
    address    VARCHAR(300),
    win_count  INT          NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_winning_stores_round_grade (round, grade)
);
