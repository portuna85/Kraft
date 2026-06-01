ALTER TABLE winning_numbers ADD COLUMN second_prize   BIGINT NOT NULL DEFAULT 0;
ALTER TABLE winning_numbers ADD COLUMN second_winners INT    NOT NULL DEFAULT 0;
ALTER TABLE winning_numbers ADD CONSTRAINT chk_wn_second_prize_nonneg   CHECK (second_prize >= 0);
ALTER TABLE winning_numbers ADD CONSTRAINT chk_wn_second_winners_nonneg CHECK (second_winners >= 0);
