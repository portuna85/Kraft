-- 커뮤니티 OAuth 사용자. email은 §5.1 PII 최소화 원칙에 따라 저장하지 않는다.
-- provider_id는 provider별 신원 문자열을 대소문자 구분 정확 매칭해야 하므로
-- utf8mb4_bin collation을 명시한다.
CREATE TABLE community_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(190) COLLATE utf8mb4_bin NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    withdrawn_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_community_users_provider (provider, provider_id)
);
