package com.kraft.community.comment;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;

public record CommunityCommentListResult(Page<CommunityComment> topLevel,
                                          Map<Long, List<CommunityComment>> repliesByParentId) {
}
