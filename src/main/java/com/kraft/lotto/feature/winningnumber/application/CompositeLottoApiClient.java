package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * primary 실패(예외) 시 fallback으로 체이닝하는 데코레이터.
 * primary가 {@link Optional#empty()}를 반환(미추첨 권위)하면 폴백하지 않는다.
 * primary가 성공했지만 2등(secondPrize)이 0인 경우, fallback에서 2등 데이터만 보충(enrich)한다.
 * DhLottery getLottoNumber API는 1등 정보만 반환하며 2등 이하는 포함하지 않는다.
 */
final class CompositeLottoApiClient implements LottoApiClient {

    private static final Logger log = LoggerFactory.getLogger(CompositeLottoApiClient.class);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final LottoApiClient primary;
    private final LottoApiClient fallback;
    private final String primaryName;
    private final String fallbackName;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    CompositeLottoApiClient(LottoApiClient primary, String primaryName,
                            LottoApiClient fallback, String fallbackName,
                            MeterRegistry meterRegistry) {
        this(primary, primaryName, fallback, fallbackName, meterRegistry, Clock.systemDefaultZone());
    }

    CompositeLottoApiClient(LottoApiClient primary, String primaryName,
                            LottoApiClient fallback, String fallbackName,
                            MeterRegistry meterRegistry, Clock clock) {
        this.primary = primary;
        this.primaryName = primaryName;
        this.fallback = fallback;
        this.fallbackName = fallbackName;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        Counter.builder("kraft.api.fallback.exhausted").register(meterRegistry);
    }

    @Override
    public Optional<WinningNumber> fetch(int round) {
        try {
            Optional<WinningNumber> result = primary.fetch(round);
            if (result.isPresent() && result.get().secondPrize() == 0) {
                result = tryEnrich(round, result.get());
            }
            return result;
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

    private Optional<WinningNumber> tryEnrich(int round, WinningNumber base) {
        if (base.drawDate().equals(LocalDate.now(clock.withZone(KST)))) {
            meterRegistry.counter("kraft.api.fallback.enrich.skipped",
                    "from", primaryName, "to", fallbackName, "reason", "draw_today").increment();
            log.debug("[{}] 2등 보충 스킵 — 당일 추첨 (round={})", primaryName, round);
            return Optional.of(base);
        }
        try {
            meterRegistry.counter("kraft.api.fallback.enrich.attempt",
                    "from", primaryName, "to", fallbackName).increment();
            Optional<WinningNumber> enrichSource = fallback.fetch(round);
            if (enrichSource.isPresent() && enrichSource.get().secondPrize() > 0) {
                WinningNumber src = enrichSource.get();
                WinningNumber enriched = new WinningNumber(
                        base.round(), base.drawDate(), base.combination(), base.bonusNumber(),
                        base.firstPrize(), base.firstWinners(), base.totalSales(), base.firstAccumAmount(),
                        src.secondPrize(), src.secondWinners(),
                        base.rawJson(), base.fetchedAt());
                meterRegistry.counter("kraft.api.fallback.enrich.success",
                        "from", primaryName, "to", fallbackName).increment();
                log.info("[{}] 2등 보충 성공 (from [{}]): round={}, secondPrize={}, secondWinners={}",
                        primaryName, fallbackName, round, enriched.secondPrize(), enriched.secondWinners());
                return Optional.of(enriched);
            }
        } catch (RuntimeException ex) {
            log.debug("[{}] 2등 보충 실패 (fallback=[{}], round={}): {}",
                    primaryName, fallbackName, round, ex.getMessage());
        }
        return Optional.of(base);
    }
}
