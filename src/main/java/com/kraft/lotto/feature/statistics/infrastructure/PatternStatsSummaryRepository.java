package com.kraft.lotto.feature.statistics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternStatsSummaryRepository
        extends JpaRepository<PatternStatsSummaryEntity, PatternStatsSummaryEntity.Id> {

    List<PatternStatsSummaryEntity> findAllByOrderByStatTypeAscBucketKeyAsc();
}
