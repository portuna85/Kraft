UPDATE winning_stores SET address         = '' WHERE address IS NULL;
UPDATE winning_stores SET purchase_method = '' WHERE purchase_method IS NULL;
UPDATE winning_stores SET source          = '' WHERE source IS NULL;

ALTER TABLE winning_stores ALTER COLUMN address         VARCHAR(300) NOT NULL DEFAULT '';
ALTER TABLE winning_stores ALTER COLUMN purchase_method VARCHAR(20)  NOT NULL DEFAULT '';
ALTER TABLE winning_stores ALTER COLUMN source          VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE winning_stores DROP CONSTRAINT IF EXISTS uk_winning_store_dedupe;
ALTER TABLE winning_stores ADD CONSTRAINT uk_winning_store_dedupe
    UNIQUE (round, grade, name, address, purchase_method, source);
