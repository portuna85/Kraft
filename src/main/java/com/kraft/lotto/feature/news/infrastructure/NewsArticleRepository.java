package com.kraft.lotto.feature.news.infrastructure;

import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsArticleRepository extends JpaRepository<NewsArticleEntity, Long> {

    boolean existsByLinkHash(String linkHash);

    Page<NewsArticleEntity> findAllByApprovedTrueOrderByPubDateDescCollectedAtDesc(Pageable pageable);

    Page<NewsArticleEntity> findAllByApprovedTrueAndSourceTierOrderByPubDateDescCollectedAtDesc(
            NewsSourceTier sourceTier,
            Pageable pageable);

    Page<NewsArticleEntity> findAllByApprovedTrueAndSourceTierInOrderByPubDateDescCollectedAtDesc(
            List<NewsSourceTier> tiers,
            Pageable pageable);

    Page<NewsArticleEntity> findAllByApprovedFalseAndRejectedFalseOrderByCollectedAtDesc(Pageable pageable);

    Page<NewsArticleEntity> findAllByRejectedTrueOrderByCollectedAtDesc(Pageable pageable);

    long countByApprovedFalse();

    @Modifying
    @Query("update NewsArticleEntity n set n.approved = false, n.rejected = true where n.sourceDomain = :domain")
    int rejectAllBySourceDomain(@Param("domain") String domain);

    @Query("select n from NewsArticleEntity n where n.approved = true and n.collectedAt >= :since order by n.collectedAt desc")
    List<NewsArticleEntity> findApprovedSince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from NewsArticleEntity n where n.collectedAt < :cutoff")
    int deleteByCollectedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
