package com.kraft.statistics;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionPairSummaryRepository extends JpaRepository<CompanionPairSummary, Long> {

    List<CompanionPairSummary> findAllByOrderByCoCountDescBallAAscBallBAsc(Pageable pageable);

    Optional<CompanionPairSummary> findByBallAAndBallB(Integer ballA, Integer ballB);
}
