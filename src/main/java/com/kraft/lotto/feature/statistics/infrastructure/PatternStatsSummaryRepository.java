package com.kraft.lotto.feature.statistics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PatternStatsSummaryRepository
        extends JpaRepository<PatternStatsSummaryEntity, PatternStatsSummaryEntity.Id> {

    @Query("SELECT e FROM PatternStatsSummaryEntity e ORDER BY e.id.statType ASC, e.id.bucketKey ASC")
    List<PatternStatsSummaryEntity> findAllByOrderByStatTypeAscBucketKeyAsc();
}
