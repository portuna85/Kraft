package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberMapper;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class WinningNumberUpsertExecutor {

    private final WinningNumberRepository repository;
    private final Clock clock;

    WinningNumberUpsertExecutor(WinningNumberRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    UpsertOutcome upsertOnce(WinningNumber wn) {
        LocalDateTime now = LocalDateTime.now(clock);
        return repository.findById(wn.round())
                .map(existing -> {
                    WinningNumberEntity incoming = WinningNumberMapper.toEntity(wn, now);
                    if (isSame(existing, incoming)) {
                        return UpsertOutcome.UNCHANGED;
                    }
                    existing.updateFrom(incoming, now);
                    return UpsertOutcome.UPDATED;
                })
                .orElseGet(() -> {
                    repository.saveAndFlush(WinningNumberMapper.toEntity(wn, now));
                    return UpsertOutcome.INSERTED;
                });
    }

    static boolean isSame(WinningNumberEntity existing, WinningNumberEntity incoming) {
        return Objects.equals(existing.getDrawDate(), incoming.getDrawDate())
                && Objects.equals(existing.getN1(), incoming.getN1())
                && Objects.equals(existing.getN2(), incoming.getN2())
                && Objects.equals(existing.getN3(), incoming.getN3())
                && Objects.equals(existing.getN4(), incoming.getN4())
                && Objects.equals(existing.getN5(), incoming.getN5())
                && Objects.equals(existing.getN6(), incoming.getN6())
                && Objects.equals(existing.getBonusNumber(), incoming.getBonusNumber())
                && Objects.equals(existing.getFirstPrize(), incoming.getFirstPrize())
                && Objects.equals(existing.getFirstWinners(), incoming.getFirstWinners())
                && Objects.equals(existing.getTotalSales(), incoming.getTotalSales())
                && Objects.equals(existing.getFirstAccumAmount(), incoming.getFirstAccumAmount());
    }
}
