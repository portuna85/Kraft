package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * primary 클라이언트가 빈 List를 반환하거나 예외를 던지면 fallback을 시도하는 데코레이터.
 * 모든 클라이언트가 실패해도 예외를 전파하지 않고 빈 List를 반환한다.
 */
final class CompositeWinningStoreApiClient implements WinningStoreApiClient {

    private static final Logger log = LoggerFactory.getLogger(CompositeWinningStoreApiClient.class);

    private final WinningStoreApiClient primary;
    private final WinningStoreApiClient fallback;
    private final String primaryName;
    private final String fallbackName;
    private final MeterRegistry meterRegistry;

    CompositeWinningStoreApiClient(WinningStoreApiClient primary, String primaryName,
                                   WinningStoreApiClient fallback, String fallbackName,
                                   MeterRegistry meterRegistry) {
        this.primary      = primary;
        this.primaryName  = primaryName;
        this.fallback     = fallback;
        this.fallbackName = fallbackName;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public List<WinningStore> fetchStores(int round, int grade) {
        boolean usedFallback = false;
        List<WinningStore> result = List.of();

        try {
            result = primary.fetchStores(round, grade);
        } catch (Exception ex) {
            log.warn("[{}] primary store fetch failed for round={}, grade={}, trying fallback [{}]: {}",
                    primaryName, round, grade, fallbackName, ex.getMessage());
            usedFallback = true;
            incrementFallbackUsed(grade);
        }

        if (!result.isEmpty()) {
            return result;
        }
        if (!usedFallback) {
            log.info("[{}] primary returned empty stores for round={}, grade={}, trying fallback [{}]",
                    primaryName, round, grade, fallbackName);
            incrementFallbackUsed(grade);
        }

        try {
            List<WinningStore> fallbackResult = fallback.fetchStores(round, grade);
            if (fallbackResult.isEmpty()) {
                incrementFallbackExhausted(grade);
            }
            return fallbackResult;
        } catch (Exception ex) {
            log.warn("[{}] fallback store fetch also failed: round={}, grade={}, reason={}",
                    fallbackName, round, grade, ex.getMessage());
            incrementFallbackExhausted(grade);
            return List.of();
        }
    }

    private void incrementFallbackUsed(int grade) {
        meterRegistry.counter("kraft.store.fallback.used",
                "from", primaryName, "to", fallbackName,
                "grade", String.valueOf(grade)).increment();
    }

    private void incrementFallbackExhausted(int grade) {
        meterRegistry.counter("kraft.store.fallback.exhausted",
                "grade", String.valueOf(grade)).increment();
    }
}
