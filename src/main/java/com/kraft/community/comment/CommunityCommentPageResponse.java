package com.kraft.community.comment;

import java.util.List;

public record CommunityCommentPageResponse(
        List<CommunityCommentResponse> topLevel,
        long totalTopLevelComments,
        int page,
        int size,
        int totalPages) {
}
