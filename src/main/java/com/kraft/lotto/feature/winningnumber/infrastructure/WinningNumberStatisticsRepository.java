package com.kraft.lotto.feature.winningnumber.infrastructure;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WinningNumberStatisticsRepository {

    @Query("""
            select w.n1 as n1, w.n2 as n2, w.n3 as n3,
                   w.n4 as n4, w.n5 as n5, w.n6 as n6
            from WinningNumberEntity w
            order by w.round asc
            """)
    List<CombinationRow> findAllCombinationsOrderByRoundAsc();

    @Query(value = """
            SELECT sub.ball AS ball, SUM(sub.hit) AS hitCount
            FROM (
                SELECT n1 AS ball, COUNT(*) AS hit FROM winning_numbers GROUP BY n1
                UNION ALL
                SELECT n2 AS ball, COUNT(*) AS hit FROM winning_numbers GROUP BY n2
                UNION ALL
                SELECT n3 AS ball, COUNT(*) AS hit FROM winning_numbers GROUP BY n3
                UNION ALL
                SELECT n4 AS ball, COUNT(*) AS hit FROM winning_numbers GROUP BY n4
                UNION ALL
                SELECT n5 AS ball, COUNT(*) AS hit FROM winning_numbers GROUP BY n5
                UNION ALL
                SELECT n6 AS ball, COUNT(*) AS hit FROM winning_numbers GROUP BY n6
            ) sub
            GROUP BY sub.ball
            ORDER BY sub.ball ASC
            """, nativeQuery = true)
    List<BallFrequencyRow> findBallFrequencies();

    @Query(value = """
            select sub.round as round, sub.draw_date as drawDate, sub.prize_rank as prizeRank
            from (
                select w.round, w.draw_date, 1 as prize_rank
                from winning_numbers w
                where w.n1 in (:n1, :n2, :n3, :n4, :n5, :n6)
                  and w.n2 in (:n1, :n2, :n3, :n4, :n5, :n6)
                  and w.n3 in (:n1, :n2, :n3, :n4, :n5, :n6)
                  and w.n4 in (:n1, :n2, :n3, :n4, :n5, :n6)
                  and w.n5 in (:n1, :n2, :n3, :n4, :n5, :n6)
                  and w.n6 in (:n1, :n2, :n3, :n4, :n5, :n6)
                union all
                select w.round, w.draw_date, 2 as prize_rank
                from winning_numbers w
                where (
                    (case when w.n1 in (:n1, :n2, :n3, :n4, :n5, :n6) then 1 else 0 end)
                  + (case when w.n2 in (:n1, :n2, :n3, :n4, :n5, :n6) then 1 else 0 end)
                  + (case when w.n3 in (:n1, :n2, :n3, :n4, :n5, :n6) then 1 else 0 end)
                  + (case when w.n4 in (:n1, :n2, :n3, :n4, :n5, :n6) then 1 else 0 end)
                  + (case when w.n5 in (:n1, :n2, :n3, :n4, :n5, :n6) then 1 else 0 end)
                  + (case when w.n6 in (:n1, :n2, :n3, :n4, :n5, :n6) then 1 else 0 end)
                ) = 5
                  and w.bonus_number in (:n1, :n2, :n3, :n4, :n5, :n6)
            ) sub
            order by sub.round desc
            """, nativeQuery = true)
    List<PrizeHitWithRankRow> findPrizeHitsByNumbers(
            Integer n1, Integer n2, Integer n3, Integer n4, Integer n5, Integer n6);

    @Query(value = """
            SELECT sub.ball AS ball, SUM(sub.hit) AS hitCount
            FROM (
                SELECT n1 AS ball, COUNT(*) AS hit FROM winning_numbers WHERE round >= :minRound GROUP BY n1
                UNION ALL
                SELECT n2, COUNT(*) FROM winning_numbers WHERE round >= :minRound GROUP BY n2
                UNION ALL
                SELECT n3, COUNT(*) FROM winning_numbers WHERE round >= :minRound GROUP BY n3
                UNION ALL
                SELECT n4, COUNT(*) FROM winning_numbers WHERE round >= :minRound GROUP BY n4
                UNION ALL
                SELECT n5, COUNT(*) FROM winning_numbers WHERE round >= :minRound GROUP BY n5
                UNION ALL
                SELECT n6, COUNT(*) FROM winning_numbers WHERE round >= :minRound GROUP BY n6
            ) sub
            GROUP BY sub.ball
            ORDER BY sub.ball ASC
            """, nativeQuery = true)
    List<BallFrequencyRow> findBallFrequenciesFromRound(@Param("minRound") int minRound);

    @Query("select count(w) from WinningNumberEntity w where w.round >= :minRound")
    long countDrawsFromRound(@Param("minRound") int minRound);

    @Query(value = """
            SELECT
                (CASE WHEN n1 % 2 = 1 THEN 1 ELSE 0 END +
                 CASE WHEN n2 % 2 = 1 THEN 1 ELSE 0 END +
                 CASE WHEN n3 % 2 = 1 THEN 1 ELSE 0 END +
                 CASE WHEN n4 % 2 = 1 THEN 1 ELSE 0 END +
                 CASE WHEN n5 % 2 = 1 THEN 1 ELSE 0 END +
                 CASE WHEN n6 % 2 = 1 THEN 1 ELSE 0 END) AS oddCount,
                COUNT(*) AS drawCount
            FROM winning_numbers
            GROUP BY oddCount
            ORDER BY oddCount
            """, nativeQuery = true)
    List<OddEvenRow> findOddEvenDistribution();

    @Query(value = """
            SELECT (n1+n2+n3+n4+n5+n6) AS totalSum, COUNT(*) AS drawCount
            FROM winning_numbers
            GROUP BY totalSum
            ORDER BY totalSum
            """, nativeQuery = true)
    List<SumRow> findSumDistribution();

    @Query(value = """
            SELECT other_ball AS otherBall, COUNT(*) AS hitCount
            FROM (
                SELECT n1 AS other_ball FROM winning_numbers WHERE :target IN (n1,n2,n3,n4,n5,n6) AND n1 != :target
                UNION ALL
                SELECT n2 FROM winning_numbers WHERE :target IN (n1,n2,n3,n4,n5,n6) AND n2 != :target
                UNION ALL
                SELECT n3 FROM winning_numbers WHERE :target IN (n1,n2,n3,n4,n5,n6) AND n3 != :target
                UNION ALL
                SELECT n4 FROM winning_numbers WHERE :target IN (n1,n2,n3,n4,n5,n6) AND n4 != :target
                UNION ALL
                SELECT n5 FROM winning_numbers WHERE :target IN (n1,n2,n3,n4,n5,n6) AND n5 != :target
                UNION ALL
                SELECT n6 FROM winning_numbers WHERE :target IN (n1,n2,n3,n4,n5,n6) AND n6 != :target
            ) sub
            GROUP BY other_ball
            ORDER BY hitCount DESC
            """, nativeQuery = true)
    List<CompanionRow> findCompanionNumbers(@Param("target") int target);

    @Query(value = """
            SELECT src_ball AS ball, other_ball AS otherBall, COUNT(*) AS hitCount
            FROM (
                SELECT n1 AS src_ball, n2 AS other_ball FROM winning_numbers
                UNION ALL SELECT n1, n3 FROM winning_numbers
                UNION ALL SELECT n1, n4 FROM winning_numbers
                UNION ALL SELECT n1, n5 FROM winning_numbers
                UNION ALL SELECT n1, n6 FROM winning_numbers
                UNION ALL SELECT n2, n1 FROM winning_numbers
                UNION ALL SELECT n2, n3 FROM winning_numbers
                UNION ALL SELECT n2, n4 FROM winning_numbers
                UNION ALL SELECT n2, n5 FROM winning_numbers
                UNION ALL SELECT n2, n6 FROM winning_numbers
                UNION ALL SELECT n3, n1 FROM winning_numbers
                UNION ALL SELECT n3, n2 FROM winning_numbers
                UNION ALL SELECT n3, n4 FROM winning_numbers
                UNION ALL SELECT n3, n5 FROM winning_numbers
                UNION ALL SELECT n3, n6 FROM winning_numbers
                UNION ALL SELECT n4, n1 FROM winning_numbers
                UNION ALL SELECT n4, n2 FROM winning_numbers
                UNION ALL SELECT n4, n3 FROM winning_numbers
                UNION ALL SELECT n4, n5 FROM winning_numbers
                UNION ALL SELECT n4, n6 FROM winning_numbers
                UNION ALL SELECT n5, n1 FROM winning_numbers
                UNION ALL SELECT n5, n2 FROM winning_numbers
                UNION ALL SELECT n5, n3 FROM winning_numbers
                UNION ALL SELECT n5, n4 FROM winning_numbers
                UNION ALL SELECT n5, n6 FROM winning_numbers
                UNION ALL SELECT n6, n1 FROM winning_numbers
                UNION ALL SELECT n6, n2 FROM winning_numbers
                UNION ALL SELECT n6, n3 FROM winning_numbers
                UNION ALL SELECT n6, n4 FROM winning_numbers
                UNION ALL SELECT n6, n5 FROM winning_numbers
            ) sub
            GROUP BY src_ball, other_ball
            ORDER BY ball ASC, hitCount DESC
            """, nativeQuery = true)
    List<CompanionPairRow> findAllCompanionPairs();

    interface BallFrequencyRow {
        Integer getBall();
        Long getHitCount();
    }

    interface CombinationRow {
        Integer getN1();
        Integer getN2();
        Integer getN3();
        Integer getN4();
        Integer getN5();
        Integer getN6();
    }

    interface PrizeHitWithRankRow {
        Integer getRound();
        LocalDate getDrawDate();
        Integer getPrizeRank();
    }

    interface OddEvenRow {
        Integer getOddCount();
        Long getDrawCount();
    }

    interface SumRow {
        Integer getTotalSum();
        Long getDrawCount();
    }

    interface CompanionRow {
        Integer getOtherBall();
        Long getHitCount();
    }

    interface CompanionPairRow {
        Integer getBall();
        Integer getOtherBall();
        Long getHitCount();
    }
}
