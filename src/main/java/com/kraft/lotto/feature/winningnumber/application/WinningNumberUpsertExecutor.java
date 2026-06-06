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

    // public: Spring AOP 프록시가 @Transactional(REQUIRES_NEW)를 적용하려면 public 필요
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UpsertOutcome upsertOnce(WinningNumber wn) {
        LocalDateTime now = LocalDateTime.now(clock);
        WinningNumberEntity e = WinningNumberMapper.toEntity(wn, now);

        boolean existed = repository.existsByRound(e.getRound());

        repository.nativeUpsert(
                e.getRound(), e.getDrawDate(),
                e.getN1(), e.getN2(), e.getN3(), e.getN4(), e.getN5(), e.getN6(),
                e.getBonusNumber(),
                e.getFirstPrize(), e.getFirstWinners(),
                e.getTotalSales(), e.getFirstAccumAmount(),
                e.getSecondPrize(), e.getSecondWinners(),
                e.getRawJson(), e.getFetchedAt(),
                now, now
        );

        if (!existed) return UpsertOutcome.INSERTED;

        // @Modifying(clearAutomatically = true)로 캐시 클리어 후 실제 version 조회
        // version > 0 이면 데이터 변경이 있었음 (UPDATED), 0 이면 동일 데이터 (UNCHANGED)
        int version = repository.findVersionByRound(e.getRound()).orElse(0);
        return version > 0 ? UpsertOutcome.UPDATED : UpsertOutcome.UNCHANGED;
    }
}
