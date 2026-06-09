ALTER TABLE news_articles ADD COLUMN IF NOT EXISTS title_hash CHAR(64) NULL;

UPDATE news_articles SET title_hash = SHA2(title, 256) WHERE title_hash IS NULL;

ALTER TABLE news_articles MODIFY COLUMN title_hash CHAR(64) NOT NULL;

CREATE INDEX IF NOT EXISTS idx_news_title_hash ON news_articles (title_hash);
