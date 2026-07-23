package com.kraft.community.post;

import com.kraft.community.auth.CommunityPrincipal;
import com.kraft.community.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/community/posts")
public class CommunityPostController {

    private final CommunityPostService communityPostService;

    public CommunityPostController(CommunityPostService communityPostService) {
        this.communityPostService = communityPostService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CommunityPostResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CommunityPostResponse> result = communityPostService.list(page, size)
                .map(CommunityPostResponse::from);
        return ResponseEntity.ok().body(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityPostResponse> detail(@PathVariable Long id) {
        CommunityPost post = communityPostService.get(id);
        return ResponseEntity.ok()
                .eTag(String.valueOf(post.getVersion()))
                .body(CommunityPostResponse.from(post));
    }

    @PostMapping
    public ResponseEntity<CommunityPostResponse> create(
            @AuthenticationPrincipal CommunityPrincipal principal,
            @Valid @RequestBody CreatePostRequest request) {
        CommunityPost post = communityPostService.create(principal.getUserId(), principal.getNickname(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(post.getId())
                .toUri();
        return ResponseEntity.created(location)
                .eTag(String.valueOf(post.getVersion()))
                .body(CommunityPostResponse.from(post));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityPostResponse> update(
            @AuthenticationPrincipal CommunityPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        CommunityPost post = communityPostService.update(principal.getUserId(), id, request);
        return ResponseEntity.ok()
                .eTag(String.valueOf(post.getVersion()))
                .cacheControl(CacheControl.noStore())
                .body(CommunityPostResponse.from(post));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CommunityPrincipal principal,
            @PathVariable Long id) {
        communityPostService.delete(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
