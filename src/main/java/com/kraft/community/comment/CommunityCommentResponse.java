package com.kraft.community.comment;

import java.time.OffsetDateTime;

public record CommunityCommentResponse(
        Long id,
        Long postId,
        Long parentId,
        Long ownerId,
        String authorNickname,
        String content,
        boolean deleted,
        OffsetDateTime createdAt
) {

    private static final String TOMBSTONE_CONTENT = "삭제된 댓글입니다.";
    private static final String TOMBSTONE_AUTHOR = "(삭제됨)";

    // tombstone된 댓글은 행은 유지하되 프런트에는 마스킹된 값만 노출한다(§3-3) —
    // ownerId는 소유권 판정용으로 남겨두되, 개인 식별 가능한 닉네임/본문은 감춘다.
    public static CommunityCommentResponse from(CommunityComment comment) {
        boolean deleted = comment.isDeleted();
        return new CommunityCommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getParentId(),
                comment.getOwnerId(),
                deleted ? TOMBSTONE_AUTHOR : comment.getAuthorNameSnapshot(),
                deleted ? TOMBSTONE_CONTENT : comment.getContent(),
                deleted,
                comment.getCreatedAt());
    }
}
