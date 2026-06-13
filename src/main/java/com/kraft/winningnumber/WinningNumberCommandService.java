package com.kraft.winningnumber;

import com.kraft.common.lotto.LottoNumberCodec;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WinningNumberCommandService {

    private final WinningNumberRepository winningNumberRepository;
    private final LottoNumberCodec lottoNumberCodec;
    private final Clock clock;

    public WinningNumberCommandService(WinningNumberRepository winningNumberRepository,
                                       LottoNumberCodec lottoNumberCodec,
                                       Clock clock) {
        this.winningNumberRepository = winningNumberRepository;
        this.lottoNumberCodec = lottoNumberCodec;
        this.clock = clock;
    }

    public WinningNumberResponse upsert(WinningNumberUpsertRequest request) {
        var normalized = lottoNumberCodec.normalize(request.numbers());

        WinningNumber winningNumber = winningNumberRepository.findByRound(request.round())
                .map(existing -> updateExisting(existing, request, normalized))
                .orElseGet(() -> createNew(request, normalized));

        return WinningNumberResponse.from(winningNumberRepository.save(winningNumber));
    }

    private WinningNumber updateExisting(WinningNumber existing,
                                         WinningNumberUpsertRequest request,
                                         java.util.List<Integer> normalized) {
        existing.update(
                request.drawDate(),
                normalized.get(0),
                normalized.get(1),
                normalized.get(2),
                normalized.get(3),
                normalized.get(4),
                normalized.get(5),
                request.bonusNumber(),
                request.firstPrizeAmount(),
                orZero(request.secondPrize()),
                orZeroInt(request.secondWinners()),
                orZero(request.totalSales()),
                orZero(request.firstAccumAmount()),
                request.rawJson()
        );
        return existing;
    }

    private WinningNumber createNew(WinningNumberUpsertRequest request, java.util.List<Integer> normalized) {
        return new WinningNumber(
                request.round(),
                request.drawDate(),
                normalized.get(0),
                normalized.get(1),
                normalized.get(2),
                normalized.get(3),
                normalized.get(4),
                normalized.get(5),
                request.bonusNumber(),
                request.firstPrizeAmount(),
                orZero(request.secondPrize()),
                orZeroInt(request.secondWinners()),
                orZero(request.totalSales()),
                orZero(request.firstAccumAmount()),
                request.rawJson(),
                OffsetDateTime.now(clock)
        );
    }

    private static long orZero(Long value) {
        return value != null ? value : 0L;
    }

    private static int orZeroInt(Integer value) {
        return value != null ? value : 0;
    }
}
