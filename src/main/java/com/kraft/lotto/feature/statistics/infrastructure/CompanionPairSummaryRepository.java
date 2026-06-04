package com.kraft.lotto.feature.statistics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionPairSummaryRepository
        extends JpaRepository<CompanionPairSummaryEntity, CompanionPairSummaryEntity.Id> {

    @Query("SELECT c FROM CompanionPairSummaryEntity c WHERE c.id.ball = :ball ORDER BY c.hitCount DESC")
    List<CompanionPairSummaryEntity> findByBallOrderByHitCountDesc(@Param("ball") int ball);
}
