ALTER TABLE winning_numbers
    ADD COLUMN second_prize       BIGINT NOT NULL DEFAULT 0 AFTER first_prize_amount,
    ADD COLUMN second_winners     INT    NOT NULL DEFAULT 0 AFTER second_prize,
    ADD COLUMN total_sales        BIGINT NOT NULL DEFAULT 0 AFTER second_winners,
    ADD COLUMN first_accum_amount BIGINT NOT NULL DEFAULT 0 AFTER total_sales,
    ADD COLUMN raw_json           TEXT   NULL     AFTER first_accum_amount,
    ADD COLUMN version            BIGINT NOT NULL DEFAULT 0 AFTER raw_json;
