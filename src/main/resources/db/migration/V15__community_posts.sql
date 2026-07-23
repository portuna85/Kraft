-- 게시글은 소유자 FK와 별개로 작성 시점 작성자명 스냅샷을 보존한다(계정 탈퇴 후에도
-- 게시글 표시가 깨지지 않도록). owner_id는 RESTRICT로 막아 게시글이 남아있는 계정은
-- 하드 삭제를 막는다(§5.1 수명주기 정책은 pseudonymization으로 별도 처리).
CREATE TABLE community_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    author_name_snapshot VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_community_posts_owner FOREIGN KEY (owner_id)
        REFERENCES community_users (id) ON DELETE RESTRICT,
    KEY idx_community_posts_created (created_at, id),
    KEY idx_community_posts_owner_created (owner_id, created_at)
);
