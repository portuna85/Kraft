package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
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
        return upsertWithResult(request).response();
    }

    public WinningNumberUpsertResult upsertWithResult(WinningNumberUpsertRequest request) {
        var normalized = lottoNumberCodec.normalize(request.numbers());
        if (normalized.contains(request.bonusNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BONUS_NUMBER",
                    "보너스 번호는 당첨 번호 6개와 중복될 수 없습니다.");
        }

        boolean[] changed = {false};
        WinningNumber winningNumber = winningNumberRepository.findByRound(request.round())
                .map(existing -> {
                    changed[0] = hasChanges(existing, request, normalized);
                    if (changed[0]) {
                        updateExisting(existing, request, normalized);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    changed[0] = true;
                    return createNew(request, normalized);
                });

        WinningNumberResponse response = WinningNumberResponse.from(winningNumberRepository.save(winningNumber));
        return new WinningNumberUpsertResult(response, changed[0]);
    }

    private boolean hasChanges(WinningNumber existing, WinningNumberUpsertRequest request,
                               java.util.List<Integer> normalized) {
        return !existing.getDrawDate().equals(request.drawDate())
                || !existing.getN1().equals(normalized.get(0))
                || !existing.getN2().equals(normalized.get(1))
                || !existing.getN3().equals(normalized.get(2))
                || !existing.getN4().equals(normalized.get(3))
                || !existing.getN5().equals(normalized.get(4))
                || !existing.getN6().equals(normalized.get(5))
                || !existing.getBonusNumber().equals(request.bonusNumber())
                || !existing.getFirstPrizeAmount().equals(request.firstPrizeAmount())
                || existing.getSecondPrize() != orZero(request.secondPrize())
                || existing.getSecondWinners() != orZeroInt(request.secondWinners())
                || existing.getTotalSales() != orZero(request.totalSales())
                || existing.getFirstAccumAmount() != orZero(request.firstAccumAmount());
    }

    private void updateExisting(WinningNumber existing,
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
                orZero(request.firstAccumAmount())
        );
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
