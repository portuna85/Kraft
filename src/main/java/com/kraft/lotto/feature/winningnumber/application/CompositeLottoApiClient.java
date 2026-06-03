package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * primary 실패(예외) 시 fallback으로 체이닝하는 데코레이터.
 * primary가 {@link Optional#empty()}를 반환(미추첨 권위)하면 폴백하지 않는다.
 */
final class CompositeLottoApiClient implements LottoApiClient {

    private static final Logger log = LoggerFactory.getLogger(CompositeLottoApiClient.class);

    private final LottoApiClient primary;
    private final LottoApiClient fallback;
    private final String primaryName;
    private final String fallbackName;
    private final MeterRegistry meterRegistry;

    CompositeLottoApiClient(LottoApiClient primary, String primaryName,
                            LottoApiClient fallback, String fallbackName,
                            MeterRegistry meterRegistry) {
        this.primary = primary;
        this.primaryName = primaryName;
        this.fallback = fallback;
        this.fallbackName = fallbackName;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<WinningNumber> fetch(int round) {
        try {
            return primary.fetch(round);
        } catch (LottoApiClientException primaryEx) {
            log.warn("[{}] primary fetch failed for round {}, trying fallback [{}]: {}",
                    primaryName, round, fallbackName, primaryEx.getMessage());
            meterRegistry.counter("kraft.api.fallback.used",
                    "from", primaryName, "to", fallbackName).increment();
            try {
                return fallback.fetch(round);
            } catch (RuntimeException fallbackEx) {
                meterRegistry.counter("kraft.api.fallback.exhausted").increment();
                log.error("[{}] fallback also failed for round {}: {}",
                        fallbackName, round, fallbackEx.getMessage());
                fallbackEx.addSuppressed(primaryEx);
                throw fallbackEx;
            }
        }
    }
}
