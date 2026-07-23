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
}
