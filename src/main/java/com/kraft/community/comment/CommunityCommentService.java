package com.kraft.community.comment;

import com.kraft.common.error.ApiException;
import com.kraft.community.post.CommunityPost;
import com.kraft.community.post.CommunityPostRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityCommentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostRepository communityPostRepository;
    private final Clock clock;

    public CommunityCommentService(CommunityCommentRepository communityCommentRepository,
                                    CommunityPostRepository communityPostRepository,
                                    Clock clock) {
        this.communityCommentRepository = communityCommentRepository;
        this.communityPostRepository = communityPostRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<CommunityComment> list(Long postId, int page, int size) {
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return communityCommentRepository.findByPostId(postId,
                PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.ASC, "createdAt", "id")));
    }

    @Transactional
    public CommunityComment create(Long ownerId, String authorNickname, Long postId, CreateCommentRequest request) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMUNITY_POST_NOT_FOUND",
                        "게시글을 찾을 수 없습니다."));

        if (request.parentId() != null) {
            CommunityComment parent = communityCommentRepository.findByIdForUpdate(request.parentId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMUNITY_COMMENT_PARENT_NOT_FOUND",
                            "답글을 달 댓글을 찾을 수 없습니다."));
            if (!parent.getPostId().equals(post.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "COMMUNITY_COMMENT_PARENT_MISMATCH",
                        "다른 게시글의 댓글에는 답글을 달 수 없습니다.");
            }
            // 1단계 대댓글만 허용 — 답글의 답글은 거부한다(§3-3).
            if (parent.getParentId() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "COMMUNITY_COMMENT_REPLY_DEPTH_EXCEEDED",
                        "답글에는 답글을 달 수 없습니다.");
            }
        }

        try {
            return communityCommentRepository.save(new CommunityComment(
                    postId, request.parentId(), ownerId, authorNickname, request.content(),
                    OffsetDateTime.now(clock)));
        } catch (DataIntegrityViolationException concurrentlyDeletedPost) {
            // 저장 직전 게시글/부모가 동시 삭제된 경합(FK race) → 사용자에게는 404로 보인다(§3-3).
            throw new ApiException(HttpStatus.NOT_FOUND, "COMMUNITY_POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다.", concurrentlyDeletedPost);
        }
    }

    @Transactional
    public void delete(Long ownerId, Long commentId) {
        CommunityComment comment = communityCommentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMUNITY_COMMENT_NOT_FOUND",
                        "댓글을 찾을 수 없습니다."));
        if (!comment.getOwnerId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMMUNITY_COMMENT_NOT_OWNER", "본인 댓글만 삭제할 수 있습니다.");
        }
        comment.markDeleted();
        communityCommentRepository.save(comment);
    }
}
