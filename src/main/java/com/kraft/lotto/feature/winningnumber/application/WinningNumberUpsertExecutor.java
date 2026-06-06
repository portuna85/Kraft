package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberMapper;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
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
        WinningNumberEntity incoming = WinningNumberMapper.toEntity(wn, now);

        // nativeUpsert 전에 현재 저장된 데이터 조회 (UNCHANGED/UPDATED 판단 기준)
        Optional<WinningNumberEntity> before = repository.findById(incoming.getRound());

        repository.nativeUpsert(
                incoming.getRound(), incoming.getDrawDate(),
                incoming.getN1(), incoming.getN2(), incoming.getN3(),
                incoming.getN4(), incoming.getN5(), incoming.getN6(),
                incoming.getBonusNumber(),
                incoming.getFirstPrize(), incoming.getFirstWinners(),
                incoming.getTotalSales(), incoming.getFirstAccumAmount(),
                incoming.getSecondPrize(), incoming.getSecondWinners(),
                incoming.getRawJson(), incoming.getFetchedAt(),
                now, now
        );

        if (before.isEmpty()) {
            return UpsertOutcome.INSERTED;
        }

        return isSameBusinessData(before.get(), incoming)
                ? UpsertOutcome.UNCHANGED
                : UpsertOutcome.UPDATED;
    }

    private boolean isSameBusinessData(WinningNumberEntity stored, WinningNumberEntity incoming) {
        return isSameLotteryNumbers(stored, incoming)
            && isSamePrizeData(stored, incoming)
            && Objects.equals(stored.getRawJson(), incoming.getRawJson());
    }

    private boolean isSameLotteryNumbers(WinningNumberEntity stored, WinningNumberEntity incoming) {
        return Objects.equals(stored.getDrawDate(),    incoming.getDrawDate())
            && Objects.equals(stored.getN1(),          incoming.getN1())
            && Objects.equals(stored.getN2(),          incoming.getN2())
            && Objects.equals(stored.getN3(),          incoming.getN3())
            && Objects.equals(stored.getN4(),          incoming.getN4())
            && Objects.equals(stored.getN5(),          incoming.getN5())
            && Objects.equals(stored.getN6(),          incoming.getN6())
            && Objects.equals(stored.getBonusNumber(), incoming.getBonusNumber());
    }

    private boolean isSamePrizeData(WinningNumberEntity stored, WinningNumberEntity incoming) {
        return Objects.equals(stored.getFirstPrize(),       incoming.getFirstPrize())
            && Objects.equals(stored.getFirstWinners(),     incoming.getFirstWinners())
            && Objects.equals(stored.getTotalSales(),       incoming.getTotalSales())
            && Objects.equals(stored.getFirstAccumAmount(), incoming.getFirstAccumAmount())
            && Objects.equals(stored.getSecondPrize(),      incoming.getSecondPrize())
            && Objects.equals(stored.getSecondWinners(),    incoming.getSecondWinners());
    }
}
