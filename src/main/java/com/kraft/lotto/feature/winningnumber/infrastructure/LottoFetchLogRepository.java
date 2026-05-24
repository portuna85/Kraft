package com.kraft.lotto.feature.winningnumber.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LottoFetchLogRepository extends JpaRepository<LottoFetchLogEntity, Long> {
    String FAILED_STATUS = "com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus.FAILED";

    long deleteByFetchedAtBefore(LocalDateTime cutoff);

    @Query("select l.id from LottoFetchLogEntity l where l.fetchedAt < :cutoff order by l.id")
    List<Long> findIdsByFetchedAtBefore(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Query("select l from LottoFetchLogEntity l where l.status = " + FAILED_STATUS + " order by l.fetchedAt desc")
    List<LottoFetchLogEntity> findRecentFailed(Pageable pageable);

    @Query("""
            select l
            from LottoFetchLogEntity l
            where l.status = com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus.FAILED
              and (:drwNoFrom is null or l.drwNo >= :drwNoFrom)
              and (:drwNoTo is null or l.drwNo <= :drwNoTo)
            order by l.fetchedAt desc
            """)
    List<LottoFetchLogEntity> findRecentFailedFiltered(
            @Param("drwNoFrom") Integer drwNoFrom,
            @Param("drwNoTo") Integer drwNoTo,
            Pageable pageable
    );

    @Query("""
            select l
            from LottoFetchLogEntity l
            where l.status = com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus.FAILED
              and (:drwNoFrom is null or l.drwNo >= :drwNoFrom)
              and (:drwNoTo is null or l.drwNo <= :drwNoTo)
              and (:reasonPattern is null or lower(l.message) like :reasonPattern)
            order by l.fetchedAt desc
            """)
    List<LottoFetchLogEntity> findRecentFailedFilteredByReason(
            @Param("drwNoFrom") Integer drwNoFrom,
            @Param("drwNoTo") Integer drwNoTo,
            @Param("reasonPattern") String reasonPattern,
            Pageable pageable
    );

    @Query("""
            select count(l)
            from LottoFetchLogEntity l
            where l.status = com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus.FAILED
              and (:drwNoFrom is null or l.drwNo >= :drwNoFrom)
              and (:drwNoTo is null or l.drwNo <= :drwNoTo)
            """)
    long countRecentFailedFiltered(
            @Param("drwNoFrom") Integer drwNoFrom,
            @Param("drwNoTo") Integer drwNoTo
    );

    @Query("""
            select count(l)
            from LottoFetchLogEntity l
            where l.status = com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchStatus.FAILED
            """)
    long countRecentFailed();

    default List<LottoFetchLogEntity> findRecentFailed(int limit) {
        return findRecentFailed(PageRequest.of(0, Math.max(1, limit)));
    }

    default List<LottoFetchLogEntity> findRecentFailedFiltered(int limit, Integer drwNoFrom, Integer drwNoTo) {
        return findRecentFailedFiltered(drwNoFrom, drwNoTo, PageRequest.of(0, Math.max(1, limit)));
    }

    default List<LottoFetchLogEntity> findRecentFailedFilteredByReason(int limit,
                                                                        Integer drwNoFrom,
                                                                        Integer drwNoTo,
                                                                        String reasonPattern) {
        return findRecentFailedFilteredByReason(
                drwNoFrom,
                drwNoTo,
                reasonPattern,
                PageRequest.of(0, Math.max(1, limit))
        );
    }
}
