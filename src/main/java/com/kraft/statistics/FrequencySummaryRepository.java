package com.kraft.statistics;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FrequencySummaryRepository extends JpaRepository<FrequencySummary, Long> {

    List<FrequencySummary> findAllByOrderByBallNumberAsc();

    Optional<FrequencySummary> findByBallNumber(Integer ballNumber);

    // BE-06: 통계 summary가 반영한 최신 회차 — summary가 비어있으면(rebuild 전) 0을 반환한다.
    @Query("select coalesce(max(f.lastRound), 0) from FrequencySummary f")
    int findMaxLastRound();
}
