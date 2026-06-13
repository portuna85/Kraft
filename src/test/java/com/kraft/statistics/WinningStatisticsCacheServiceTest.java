package com.kraft.statistics;

import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WinningStatisticsCacheServiceTest {

    @Autowired
    private WinningStatisticsCacheService service;

    @Autowired
    private WinningNumberRepository winningNumberRepository;

    @Autowired
    private FrequencySummaryRepository frequencySummaryRepository;

    @Autowired
    private PatternStatsSummaryRepository patternStatsSummaryRepository;

    @Autowired
    private CompanionPairSummaryRepository companionPairSummaryRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        frequencySummaryRepository.deleteAll();
        patternStatsSummaryRepository.deleteAll();
        companionPairSummaryRepository.deleteAll();
        winningNumberRepository.deleteAll();

        // 회차 1: 1, 2, 3, 4, 5, 6 (홀3·짝3, 저6·고0, 합21)
        winningNumberRepository.save(round(1, 1, 2, 3, 4, 5, 6, 7));
        // 회차 2: 1, 2, 10, 20, 30, 45 (홀2·짝4, 저3·고3, 합108)
        winningNumberRepository.save(round(2, 1, 2, 10, 20, 30, 45, 8));
    }

    @Test
    void rebuildAllSummaries_populatesFrequencyForAllBalls() {
        service.rebuildAllSummaries();

        List<FrequencySummary> all = frequencySummaryRepository.findAllByOrderByBallNumberAsc();
        assertThat(all).hasSize(45);

        // 볼 1은 두 회차 모두 등장
        FrequencySummary ball1 = all.stream().filter(s -> s.getBallNumber() == 1).findFirst().orElseThrow();
        assertThat(ball1.getFrequency()).isEqualTo(2);
        assertThat(ball1.getLastRound()).isEqualTo(2);

        // 볼 45는 회차 2에만 등장
        FrequencySummary ball45 = all.stream().filter(s -> s.getBallNumber() == 45).findFirst().orElseThrow();
        assertThat(ball45.getFrequency()).isEqualTo(1);
        assertThat(ball45.getLastRound()).isEqualTo(2);

        // 볼 7은 등장하지 않음 (보너스 번호는 집계 제외)
        FrequencySummary ball7 = all.stream().filter(s -> s.getBallNumber() == 7).findFirst().orElseThrow();
        assertThat(ball7.getFrequency()).isEqualTo(0);
    }

    @Test
    void rebuildAllSummaries_populatesPatternStats() {
        service.rebuildAllSummaries();

        // 회차 1: 홀수 3개(1,3,5)
        List<PatternStatsSummary> oddRows = patternStatsSummaryRepository
                .findByStatTypeOrderByBucketKeyAsc(WinningStatisticsCacheService.TYPE_ODD_COUNT);
        assertThat(oddRows).isNotEmpty();

        long countOdd3 = oddRows.stream()
                .filter(r -> "3".equals(r.getBucketKey()))
                .mapToInt(PatternStatsSummary::getCountVal).sum();
        assertThat(countOdd3).isEqualTo(1); // 회차 1만 홀수 3개
    }

    @Test
    void rebuildAllSummaries_populatesCompanionPairs() {
        service.rebuildAllSummaries();

        // 회차 1: 1-2가 동반 출현
        CompanionPairSummary pair12 = companionPairSummaryRepository
                .findByBallAAndBallB(1, 2).orElseThrow();
        assertThat(pair12.getCoCount()).isEqualTo(2); // 두 회차 모두 1, 2 등장

        // 회차 2에만 있는 1-10 쌍
        CompanionPairSummary pair110 = companionPairSummaryRepository
                .findByBallAAndBallB(1, 10).orElseThrow();
        assertThat(pair110.getCoCount()).isEqualTo(1);
    }

    @Test
    void getFrequencyStats_fallsBackToRebuildWhenEmpty() {
        FrequencyStatsResponse response = service.getFrequencyStats();

        assertThat(response.totalRounds()).isEqualTo(2);
        assertThat(response.frequencies()).hasSize(45);
    }

    @Test
    void analyze_returnsCorrectMetrics() {
        AnalysisResponse result = service.analyze(List.of(1, 2, 3, 4, 5, 6));

        assertThat(result.oddCount()).isEqualTo(3);
        assertThat(result.evenCount()).isEqualTo(3);
        assertThat(result.sumOfNumbers()).isEqualTo(21);
        assertThat(result.sumBucket()).isEqualTo("21-65");
        assertThat(result.consecutivePairCount()).isEqualTo(5); // 1-2, 2-3, 3-4, 4-5, 5-6
        assertThat(result.lowCount()).isEqualTo(6); // 모두 1-22 범위
        assertThat(result.highCount()).isEqualTo(0);
    }

    private WinningNumber round(int r, int n1, int n2, int n3, int n4, int n5, int n6, int bonus) {
        return new WinningNumber(r, LocalDate.of(2026, 1, r),
                n1, n2, n3, n4, n5, n6, bonus,
                1_000_000_000L, 0L, 0, 0L, 0L, null,
                OffsetDateTime.now(Clock.system(KST)));
    }
}
