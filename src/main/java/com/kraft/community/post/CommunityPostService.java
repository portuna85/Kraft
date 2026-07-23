package com.kraft.community.post;

import com.kraft.common.error.ApiException;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommunityPostService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CommunityPostRepository communityPostRepository;
    private final Clock clock;

    public CommunityPostService(CommunityPostRepository communityPostRepository, Clock clock) {
        this.communityPostRepository = communityPostRepository;
        this.clock = clock;
    }

    @Transactional
    public CommunityPost create(Long ownerId, String authorNickname, CreatePostRequest request) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        return communityPostRepository.save(
                new CommunityPost(ownerId, authorNickname, request.title(), request.content(), now, now));
    }

    @Transactional(readOnly = true)
    public CommunityPost get(Long postId) {
        return communityPostRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMUNITY_POST_NOT_FOUND",
                        "게시글을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<CommunityPost> list(int page, int size) {
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        return communityPostRepository.findAll(
                PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Transactional
    public CommunityPost update(Long ownerId, Long postId, UpdatePostRequest request) {
        CommunityPost post = get(postId);
        requireOwner(post, ownerId);
        if (post.getVersion() != request.expectedVersion()) {
            throw new ApiException(HttpStatus.CONFLICT, "COMMUNITY_POST_VERSION_CONFLICT",
                    "다른 곳에서 먼저 수정되었습니다. 새로고침 후 다시 시도하세요.");
        }
        post.update(request.title(), request.content(), OffsetDateTime.now(clock));
        try {
            return communityPostRepository.saveAndFlush(post);
        } catch (ObjectOptimisticLockingFailureException raceLostAfterCheck) {
            // 버전 사전 검증 이후에도 동시 수정이 끼어든 경우(§6 개선②) — 광역 예외 대신
            // 이 리소스에 특정된 409로 변환한다.
            throw new ApiException(HttpStatus.CONFLICT, "COMMUNITY_POST_VERSION_CONFLICT",
                    "다른 곳에서 먼저 수정되었습니다. 새로고침 후 다시 시도하세요.", raceLostAfterCheck);
        }
    }

    @Transactional
    public void delete(Long ownerId, Long postId) {
        CommunityPost post = get(postId);
        requireOwner(post, ownerId);
        communityPostRepository.delete(post);
    }

    private void requireOwner(CommunityPost post, Long ownerId) {
        if (!post.getOwnerId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMMUNITY_POST_NOT_OWNER", "본인 게시글만 수정·삭제할 수 있습니다.");
        }
    }
}
