package com.kraft.community.comment;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    Page<CommunityComment> findByPostId(Long postId, Pageable pageable);

    // 부모 댓글에 답글을 붙이거나 삭제(tombstone)할 때 동시 경합을 막기 위한 행 잠금 조회
    // (Blitz "부모 row lock으로 삭제 경합 차단" §3-3).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommunityComment c where c.id = :id")
    Optional<CommunityComment> findByIdForUpdate(@Param("id") Long id);

    // targetPage 계산용 — 해당 게시글의 상위 댓글(parentId is null) 중 id가 :id 이하인 개수.
    // id는 IDENTITY PK라 생성 순서와 단조 증가가 일치하므로 createdAt 대신 사용해도 안전하다.
    @Query("select count(c) from CommunityComment c "
            + "where c.postId = :postId and c.parentId is null and c.id <= :id")
    long countTopLevelUpToId(@Param("postId") Long postId, @Param("id") Long id);
}
