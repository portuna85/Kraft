package com.kraft.statistics;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionPairSummaryRepository extends JpaRepository<CompanionPairSummary, Long> {

    List<CompanionPairSummary> findAllByOrderByCoCountDescBallAAscBallBAsc(Pageable pageable);

    Optional<CompanionPairSummary> findByBallAAndBallB(Integer ballA, Integer ballB);

    // 특정 번호가 포함된 쌍만 조회 — 클라이언트가 전체 990쌍을 받아 직접 필터링할 필요를 없앤다.
    List<CompanionPairSummary> findByBallAOrBallBOrderByCoCountDescBallAAscBallBAsc(Integer ballA, Integer ballB);
}
