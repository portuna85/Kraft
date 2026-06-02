package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberMapper;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
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
        return Snapshot.of(existing).equals(Snapshot.of(incoming));
    }

    private record Snapshot(
            java.time.LocalDate drawDate,
            Integer n1, Integer n2, Integer n3, Integer n4, Integer n5, Integer n6,
            Integer bonusNumber,
            Long firstPrize, Integer firstWinners, Long totalSales, Long firstAccumAmount
    ) {
        static Snapshot of(WinningNumberEntity e) {
            return new Snapshot(
                    e.getDrawDate(),
                    e.getN1(), e.getN2(), e.getN3(), e.getN4(), e.getN5(), e.getN6(),
                    e.getBonusNumber(),
                    e.getFirstPrize(), e.getFirstWinners(), e.getTotalSales(), e.getFirstAccumAmount()
            );
        }
    }
}
