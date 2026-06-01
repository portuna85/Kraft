package com.kraft.lotto.feature.winningnumber.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface WinningStoreRepository extends JpaRepository<WinningStoreEntity, Long> {

    List<WinningStoreEntity> findByRoundAndGradeOrderByIdAsc(int round, int grade);

    boolean existsByRound(int round);

    @Transactional
    @Modifying
    @Query("DELETE FROM WinningStoreEntity w WHERE w.round = :round")
    void deleteByRound(int round);
}
