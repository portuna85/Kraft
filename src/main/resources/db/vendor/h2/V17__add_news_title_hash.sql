ALTER TABLE news_articles ADD COLUMN IF NOT EXISTS title_hash CHAR(64) NULL;
UPDATE news_articles SET title_hash = LOWER(RAWTOHEX(HASH('SHA-256', STRINGTOUTF8(COALESCE(title, ''))))) WHERE title_hash IS NULL;
ALTER TABLE news_articles ALTER COLUMN title_hash CHAR(64) NOT NULL DEFAULT '';
CREATE INDEX IF NOT EXISTS idx_news_title_hash ON news_articles (title_hash);
