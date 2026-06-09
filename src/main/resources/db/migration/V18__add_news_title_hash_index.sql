CREATE INDEX idx_news_title_hash_collected_at
    ON news_articles (title_hash, collected_at);
