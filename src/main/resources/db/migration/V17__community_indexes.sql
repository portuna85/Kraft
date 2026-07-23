-- 부모 댓글 조회(1단계 reply 검증, cascade 삭제 경로) 성능을 위한 보강 인덱스.
CREATE INDEX idx_community_comments_parent ON community_comments (parent_id);
