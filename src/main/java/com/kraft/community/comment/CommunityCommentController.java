package com.kraft.community.comment;

import com.kraft.community.auth.CommunityPrincipal;
import com.kraft.community.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommunityCommentController {

    private final CommunityCommentService communityCommentService;

    public CommunityCommentController(CommunityCommentService communityCommentService) {
        this.communityCommentService = communityCommentService;
    }

    @GetMapping("/api/v1/community/posts/{postId}/comments")
    public ResponseEntity<PageResponse<CommunityCommentResponse>> list(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<CommunityCommentResponse> result = communityCommentService.list(postId, page, size)
                .map(CommunityCommentResponse::from);
        return ResponseEntity.ok().body(PageResponse.from(result));
    }

    @PostMapping("/api/v1/community/posts/{postId}/comments")
    public ResponseEntity<CommunityCommentResponse> create(
            @AuthenticationPrincipal CommunityPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommunityComment comment = communityCommentService.create(
                principal.getUserId(), principal.getNickname(), postId, request);
        return ResponseEntity.status(201).body(CommunityCommentResponse.from(comment));
    }

    @DeleteMapping("/api/v1/community/comments/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CommunityPrincipal principal,
            @PathVariable Long id) {
        communityCommentService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
