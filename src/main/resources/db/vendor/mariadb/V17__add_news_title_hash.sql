ALTER TABLE news_articles ADD COLUMN title_hash CHAR(64) NULL;

UPDATE news_articles SET title_hash = SHA2(title, 256);

ALTER TABLE news_articles MODIFY COLUMN title_hash CHAR(64) NOT NULL;

CREATE INDEX idx_news_title_hash ON news_articles (title_hash);
