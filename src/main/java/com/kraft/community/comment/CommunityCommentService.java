package com.kraft.community.comment;

import com.kraft.common.error.ApiException;
import com.kraft.community.post.CommunityPost;
import com.kraft.community.post.CommunityPostRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    // 댓글 목록 컨트롤러의 기본 페이지 크기(size=50)와 맞춰야 targetPage가 실제 목록 조회와
    // 어긋나지 않는다 — 프런트가 이 값과 다른 size로 목록을 조회하면 targetPage는 참고용이 된다.
    private static final int TARGET_PAGE_SIZE = 50;

    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostRepository communityPostRepository;
    private final Clock clock;
    private final Counter tombstoneCounter;

    public CommunityCommentService(CommunityCommentRepository communityCommentRepository,
                                    CommunityPostRepository communityPostRepository,
                                    Clock clock,
                                    MeterRegistry meterRegistry) {
        this.communityCommentRepository = communityCommentRepository;
        this.communityPostRepository = communityPostRepository;
        this.clock = clock;
        this.tombstoneCounter = Counter.builder("kraft_community_comment_tombstoned_total")
                .description("tombstone 처리(삭제)된 댓글 누적 수")
                .register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public Page<CommunityComment> list(Long postId, int page, int size) {
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return communityCommentRepository.findByPostId(postId,
                PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.ASC, "createdAt", "id")));
    }

    @Transactional
    public CommunityCommentCreationResult create(Long ownerId, String authorNickname, Long postId,
                                                   CreateCommentRequest request) {
        CommunityPost post = communityPostRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMUNITY_POST_NOT_FOUND",
                        "게시글을 찾을 수 없습니다."));

        CommunityComment parent = null;
        if (request.parentId() != null) {
            parent = communityCommentRepository.findByIdForUpdate(request.parentId())
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
            if (parent.isDeleted()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "COMMUNITY_COMMENT_PARENT_DELETED",
                        "삭제된 댓글에는 답글을 달 수 없습니다.");
            }
        }

        CommunityComment saved;
        try {
            saved = communityCommentRepository.save(new CommunityComment(
                    postId, request.parentId(), ownerId, authorNickname, request.content(),
                    OffsetDateTime.now(clock)));
        } catch (DataIntegrityViolationException concurrentlyDeletedPost) {
            // 저장 직전 게시글/부모가 동시 삭제된 경합(FK race) → 사용자에게는 404로 보인다(§3-3).
            throw new ApiException(HttpStatus.NOT_FOUND, "COMMUNITY_POST_NOT_FOUND",
                    "게시글을 찾을 수 없습니다.", concurrentlyDeletedPost);
        }

        // targetPage: 상위 댓글이면 자기 자신, 답글이면 부모(항상 상위 댓글)의 목록 내 위치를
        // 기준으로 계산한다(§6: "mutation 후 target page 계산" — Blitz의 동일 의도 이식).
        Long anchorId = parent != null ? parent.getId() : saved.getId();
        long countUpToAnchor = communityCommentRepository.countTopLevelUpToId(postId, anchorId);
        int targetPage = (int) ((Math.max(1, countUpToAnchor) - 1) / TARGET_PAGE_SIZE);

        return new CommunityCommentCreationResult(saved, targetPage);
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
        tombstoneCounter.increment();
    }
}
