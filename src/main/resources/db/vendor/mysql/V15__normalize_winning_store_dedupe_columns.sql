UPDATE winning_stores SET address         = '' WHERE address IS NULL;
UPDATE winning_stores SET purchase_method = '' WHERE purchase_method IS NULL;
UPDATE winning_stores SET source          = '' WHERE source IS NULL;

ALTER TABLE winning_stores
    MODIFY address         VARCHAR(300) NOT NULL DEFAULT '',
    MODIFY purchase_method VARCHAR(20)  NOT NULL DEFAULT '',
    MODIFY source          VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE winning_stores DROP INDEX IF EXISTS uk_winning_store_dedupe;
ALTER TABLE winning_stores ADD UNIQUE KEY IF NOT EXISTS uk_winning_store_dedupe
    (round, grade, name, address, purchase_method, source);
