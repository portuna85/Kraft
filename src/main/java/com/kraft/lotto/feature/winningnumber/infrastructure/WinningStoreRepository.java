package com.kraft.lotto.feature.winningnumber.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface WinningStoreRepository extends JpaRepository<WinningStoreEntity, Long> {

    List<WinningStoreEntity> findByRoundAndGradeOrderByIdAsc(int round, int grade);

    boolean existsByRound(int round);

    boolean existsByRoundAndGrade(int round, int grade);

    @Query("SELECT MAX(w.collectedAt) FROM WinningStoreEntity w WHERE w.round = :round")
    Optional<LocalDateTime> findLastCollectedAtByRound(@Param("round") int round);

    @Transactional
    @Modifying
    @Query("DELETE FROM WinningStoreEntity w WHERE w.round = :round")
    void deleteByRound(int round);

    @Transactional
    @Modifying
    @Query("DELETE FROM WinningStoreEntity w WHERE w.round = :round AND w.grade = :grade")
    void deleteByRoundAndGrade(@Param("round") int round, @Param("grade") int grade);
}
