package com.kraft.statistics;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatternStatsSummaryRepository extends JpaRepository<PatternStatsSummary, Long> {

    List<PatternStatsSummary> findByStatTypeOrderByBucketKeyAsc(String statType);

    Optional<PatternStatsSummary> findByStatTypeAndBucketKey(String statType, String bucketKey);
}
