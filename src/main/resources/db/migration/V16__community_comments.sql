-- 1단계 대댓글(reply of reply)만 허용한다 — DB는 자기참조 FK로 관계만 열어두고,
-- "부모가 이미 reply면 거부"는 애플리케이션 레벨에서 강제한다(§3-3).
-- deleted는 tombstone 플래그: 삭제 시 행은 유지하고 content/author를 서비스 레벨에서
-- 마스킹한다(FK 경합·페이징 번호가 삭제로 밀리지 않도록).
CREATE TABLE community_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    owner_id BIGINT NOT NULL,
    author_name_snapshot VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_community_comments_post FOREIGN KEY (post_id)
        REFERENCES community_posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_community_comments_parent FOREIGN KEY (parent_id)
        REFERENCES community_comments (id) ON DELETE CASCADE,
    CONSTRAINT fk_community_comments_owner FOREIGN KEY (owner_id)
        REFERENCES community_users (id) ON DELETE RESTRICT,
    KEY idx_community_comments_post_created (post_id, created_at)
);
