package com.kraft.lotto.feature.recommend.application;

import com.kraft.lotto.feature.recommend.domain.PastWinningCache;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.event.WinningNumbersCollectedEvent;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * PastWinningCache를 Repository로부터 적재/갱신하는 어댑터.
 * 도메인 캐시(POJO)에 Spring/JPA 의존성을 분리시키기 위한 application 계층 컴포넌트.
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Cache bean reference is intentionally shared as application state")
public class PastWinningCacheLoader {

    private static final Logger log = LoggerFactory.getLogger(PastWinningCacheLoader.class);

    private final PastWinningCache cache;
    private final WinningNumberRepository repository;

    public PastWinningCacheLoader(PastWinningCache cache, WinningNumberRepository repository) {
        this.cache = cache;
        this.repository = repository;
    }

    @PostConstruct
    public void initialize() {
        reload();
    }

    @Async
    @EventListener
    public void onCollected(WinningNumbersCollectedEvent event) {
        if (!event.dataChanged()) {
            log.debug("PastWinningCache reload skipped: dataChanged=false (collected={}, updated={}, skipped={}, failed={})",
                    event.collected(), event.updated(), event.skipped(), event.failed());
            return;
        }
        log.info("PastWinningCache reload triggered: collected={}, updated={}, skipped={}, failed={}",
                event.collected(), event.updated(), event.skipped(), event.failed());
        reload();
    }

    public void reload() {
        try {
            LoadResult result = loadSnapshot();
            cache.replace(result.combinations());
            if (result.invalidRows() > 0) {
                log.warn("PastWinningCache loaded with invalid rows skipped: skipped={}, loaded={}",
                        result.invalidRows(), cache.size());
            } else {
                log.debug("PastWinningCache loaded size={}", cache.size());
            }
        } catch (RuntimeException ex) {
            log.error("PastWinningCache reload failed; keeping previous snapshot size={}", cache.size(), ex);
        }
    }

    private LoadResult loadSnapshot() {
        List<LottoCombination> combinations = new ArrayList<>();
        int[] invalidRows = {0};
        repository.findAllCombinationsOrderByRoundAsc().forEach(row -> {
            try {
                if (row.getN1() == null || row.getN2() == null || row.getN3() == null
                        || row.getN4() == null || row.getN5() == null || row.getN6() == null) {
                    invalidRows[0]++;
                    return;
                }
                combinations.add(new LottoCombination(List.of(
                        row.getN1(), row.getN2(), row.getN3(),
                        row.getN4(), row.getN5(), row.getN6()
                )));
            } catch (RuntimeException ex) {
                invalidRows[0]++;
                log.warn("Skipping invalid winning number combination row while loading cache", ex);
            }
        });
        return new LoadResult(List.copyOf(combinations), invalidRows[0]);
    }

    private record LoadResult(List<LottoCombination> combinations, int invalidRows) {
    }
}
