package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberMapper;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
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
        WinningNumberEntity e = WinningNumberMapper.toEntity(wn, now);
        int affected = repository.nativeUpsert(
                e.getRound(), e.getDrawDate(),
                e.getN1(), e.getN2(), e.getN3(), e.getN4(), e.getN5(), e.getN6(),
                e.getBonusNumber(),
                e.getFirstPrize(), e.getFirstWinners(),
                e.getTotalSales(), e.getFirstAccumAmount(),
                e.getSecondPrize(), e.getSecondWinners(),
                e.getRawJson(), e.getFetchedAt(),
                now, now
        );
        return switch (affected) {
            case 1 -> UpsertOutcome.INSERTED;
            case 2 -> UpsertOutcome.UPDATED;
            default -> UpsertOutcome.UNCHANGED;
        };
    }
}
