package com.kraft.lotto.feature.winningnumber.infrastructure;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WinningNumberRepository extends JpaRepository<WinningNumberEntity, Integer>,
        WinningNumberStatisticsRepository {

    Optional<WinningNumberEntity> findTopByOrderByRoundDesc();

    @Query("select max(w.round) from WinningNumberEntity w")
    Optional<Integer> findMaxRound();

    @Query("select w.round from WinningNumberEntity w where w.round between :from and :to")
    Set<Integer> findRoundsBetween(int from, int to);

    boolean existsByRound(int round);

    /**
     * 회차를 원자적으로 upsert한다. 동일 회차 동시 INSERT 경쟁 조건을 DB 레벨에서 제거.
     *
     * <p>MariaDB ROW_COUNT 반환값: 1=INSERT, 2=UPDATE(값 변경), 0=UNCHANGED(값 동일)</p>
     *
     * <p>변경 여부(UNCHANGED/UPDATED)는 호출 측에서 nativeUpsert 실행 전후 데이터 비교로 판단한다.
     * created_at은 UPDATE 시 갱신하지 않는다.</p>
     */
    @Modifying(clearAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO winning_numbers
                (round, draw_date, n1, n2, n3, n4, n5, n6, bonus_number,
                 first_prize, first_winners, total_sales, first_accum_amount,
                 second_prize, second_winners, raw_json, fetched_at, created_at, updated_at, version)
            VALUES
                (:round, :drawDate, :n1, :n2, :n3, :n4, :n5, :n6, :bonusNumber,
                 :firstPrize, :firstWinners, :totalSales, :firstAccumAmount,
                 :secondPrize, :secondWinners, :rawJson, :fetchedAt, :createdAt, :updatedAt, 0)
            ON DUPLICATE KEY UPDATE
                draw_date          = VALUES(draw_date),
                n1                 = VALUES(n1),
                n2                 = VALUES(n2),
                n3                 = VALUES(n3),
                n4                 = VALUES(n4),
                n5                 = VALUES(n5),
                n6                 = VALUES(n6),
                bonus_number       = VALUES(bonus_number),
                first_prize        = VALUES(first_prize),
                first_winners      = VALUES(first_winners),
                total_sales        = VALUES(total_sales),
                first_accum_amount = VALUES(first_accum_amount),
                second_prize       = VALUES(second_prize),
                second_winners     = VALUES(second_winners),
                raw_json           = VALUES(raw_json),
                fetched_at         = VALUES(fetched_at),
                updated_at         = VALUES(updated_at),
                version            = version + 1
            """)
    int nativeUpsert(
            @Param("round") Integer round,
            @Param("drawDate") java.time.LocalDate drawDate,
            @Param("n1") Integer n1,
            @Param("n2") Integer n2,
            @Param("n3") Integer n3,
            @Param("n4") Integer n4,
            @Param("n5") Integer n5,
            @Param("n6") Integer n6,
            @Param("bonusNumber") Integer bonusNumber,
            @Param("firstPrize") Long firstPrize,
            @Param("firstWinners") Integer firstWinners,
            @Param("totalSales") Long totalSales,
            @Param("firstAccumAmount") Long firstAccumAmount,
            @Param("secondPrize") Long secondPrize,
            @Param("secondWinners") Integer secondWinners,
            @Param("rawJson") String rawJson,
            @Param("fetchedAt") LocalDateTime fetchedAt,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    Page<WinningNumberEntity> findAllByOrderByRoundDesc(Pageable pageable);
}
