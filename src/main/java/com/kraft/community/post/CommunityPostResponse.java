package com.kraft.community.post;

import java.time.OffsetDateTime;

// owner 판정에 필요한 ownerId는 포함하되, canEdit 같은 개인화 파생 필드는 넣지 않는다 —
// 소유권 판정은 클라이언트가 세션 엔드포인트의 로그인 사용자 ID와 대조해 스스로 계산한다(§4.4).
public record CommunityPostResponse(
        Long id,
        Long ownerId,
        String authorNickname,
        String title,
        String content,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CommunityPostResponse from(CommunityPost post) {
        return new CommunityPostResponse(
                post.getId(),
                post.getOwnerId(),
                post.getAuthorNameSnapshot(),
                post.getTitle(),
                post.getContent(),
                post.getVersion(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
