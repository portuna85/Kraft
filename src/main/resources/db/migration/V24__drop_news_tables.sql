-- V20에서 news_articles 에 생성한 인덱스를 먼저 제거한다
ALTER TABLE news_articles DROP INDEX idx_news_public_list;

DROP TABLE IF EXISTS news_articles;
DROP TABLE IF EXISTS news_blocked_domain;
DROP TABLE IF EXISTS news_blocked_keyword;
