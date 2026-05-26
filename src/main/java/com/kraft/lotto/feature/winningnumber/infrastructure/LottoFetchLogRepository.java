package com.kraft.lotto.feature.winningnumber.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LottoFetchLogRepository extends JpaRepository<LottoFetchLogEntity, Long> {

    long countByFetchedAtBefore(LocalDateTime cutoff);

    @Query("select l.id from LottoFetchLogEntity l where l.fetchedAt < :cutoff order by l.id")
    List<Long> findIdsByFetchedAtBefore(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Query("""
            select l
            from LottoFetchLogEntity l
            where l.status = com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus.FAILED
              and (:drwNoFrom is null or l.drwNo >= :drwNoFrom)
              and (:drwNoTo is null or l.drwNo <= :drwNoTo)
              and (
                    :reason is null
                    or lower(l.failureReason) = :reason
                    or (l.failureReason is null and lower(l.message) like concat('reason=', :reason, ';%'))
              )
            order by l.fetchedAt desc
            """)
    List<LottoFetchLogEntity> findRecentFailedFilteredByReason(
            @Param("drwNoFrom") Integer drwNoFrom,
            @Param("drwNoTo") Integer drwNoTo,
            @Param("reason") String reason,
            Pageable pageable
    );

    @Query("select min(l.fetchedAt) from LottoFetchLogEntity l")
    LocalDateTime findOldestFetchedAt();

    @Query("select max(l.fetchedAt) from LottoFetchLogEntity l")
    LocalDateTime findNewestFetchedAt();

    default List<LottoFetchLogEntity> findRecentFailedFilteredByReason(int limit,
                                                                        Integer drwNoFrom,
                                                                        Integer drwNoTo,
                                                                        String reason) {
        return findRecentFailedFilteredByReason(
                drwNoFrom,
                drwNoTo,
                reason,
                PageRequest.of(0, Math.max(1, limit))
        );
    }
}
