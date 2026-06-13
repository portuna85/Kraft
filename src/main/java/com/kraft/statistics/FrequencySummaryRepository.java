package com.kraft.statistics;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrequencySummaryRepository extends JpaRepository<FrequencySummary, Long> {

    List<FrequencySummary> findAllByOrderByBallNumberAsc();

    Optional<FrequencySummary> findByBallNumber(Integer ballNumber);
}
