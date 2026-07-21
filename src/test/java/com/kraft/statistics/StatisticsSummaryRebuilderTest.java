package com.kraft.statistics;

import com.kraft.winningnumber.WinningNumber;
import com.kraft.winningnumber.WinningNumberRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("통계 summary 리빌더 테스트")
class StatisticsSummaryRebuilderTest {

    private static final String REBUILD_LOCK_NAME = "statistics-summary-rebuild";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private StatisticsSummaryRebuilder summaryRebuilder;

    @Autowired
    private WinningNumberRepository winningNumberRepository;

    @Autowired
    private FrequencySummaryRepository frequencySummaryRepository;

    @Autowired
    private PatternStatsSummaryRepository patternStatsSummaryRepository;

    @Autowired
    private CompanionPairSummaryRepository companionPairSummaryRepository;

    @Autowired
    private LockProvider lockProvider;

    private SimpleLock heldLock;

    @BeforeEach
    void setUp() {
        frequencySummaryRepository.deleteAll();
        patternStatsSummaryRepository.deleteAll();
        companionPairSummaryRepository.deleteAll();
        winningNumberRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        if (heldLock != null) {
            heldLock.unlock();
            heldLock = null;
        }
    }

    @Test
    @DisplayName("이전 회차에만 존재하던 패턴·동반 조합은 재생성 후 스테일 행으로 삭제된다")
    void rebuildAllSummaries_removesStaleRowsNoLongerPresentInData() {
        // 회차 1: 1,3,5,7,9,11 (홀수 6개 패턴, 1-3 동반쌍 포함)
        winningNumberRepository.save(round(1, 1, 3, 5, 7, 9, 11, 2));
        summaryRebuilder.rebuildAllSummaries();

        assertThat(patternStatsSummaryRepository
                .findByStatTypeAndBucketKey(WinningStatisticsCacheService.TYPE_ODD_COUNT, "6"))
                .isPresent();
        assertThat(companionPairSummaryRepository.findByBallAAndBallB(1, 3)).isPresent();

        // 회차 1을 지우고 홀수 3개/동반쌍이 다른 회차 2만 남긴다 — "홀수 6개" 버킷과 1-3 동반쌍은
        // 더 이상 어떤 회차에도 존재하지 않으므로 재생성 시 삭제되어야 한다.
        winningNumberRepository.deleteAll();
        winningNumberRepository.save(round(2, 1, 10, 20, 30, 40, 44, 8));
        summaryRebuilder.rebuildAllSummaries();

        assertThat(patternStatsSummaryRepository
                .findByStatTypeAndBucketKey(WinningStatisticsCacheService.TYPE_ODD_COUNT, "6"))
                .isEmpty();
        assertThat(companionPairSummaryRepository.findByBallAAndBallB(1, 3)).isEmpty();
        assertThat(companionPairSummaryRepository.findByBallAAndBallB(1, 10)).isPresent();
    }

    @Test
    @DisplayName("다른 인스턴스가 락을 보유 중이면 재생성을 건너뛰고 기존 데이터를 그대로 둔다")
    void rebuildAllSummaries_skipsWhenLockAlreadyHeld() {
        winningNumberRepository.save(round(1, 1, 2, 3, 4, 5, 6, 7));

        Optional<SimpleLock> lock = lockProvider.lock(new LockConfiguration(
                Clock.system(KST).instant(), REBUILD_LOCK_NAME, Duration.ofMinutes(10), Duration.ZERO));
        assertThat(lock).isPresent();
        heldLock = lock.get();

        summaryRebuilder.rebuildAllSummaries();

        assertThat(frequencySummaryRepository.findAll()).isEmpty();
        assertThat(patternStatsSummaryRepository.findAll()).isEmpty();
        assertThat(companionPairSummaryRepository.findAll()).isEmpty();
    }

    private WinningNumber round(int r, int n1, int n2, int n3, int n4, int n5, int n6, int bonus) {
        return new WinningNumber(r, LocalDate.of(2026, 1, r),
                n1, n2, n3, n4, n5, n6, bonus,
                1_000_000_000L, 0L, 0, 0L, 0L,
                OffsetDateTime.now(Clock.system(KST)));
    }
}
