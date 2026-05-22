package com.kraft.lotto.support.fixtures;

import com.kraft.lotto.feature.recommend.web.dto.CombinationDto;
import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.domain.WinningNumber;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberMapper;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

public final class LottoTestFixtures {

    private LottoTestFixtures() {
    }

    public static WinningNumber winningNumber(int round) {
        return new WinningNumber(
                round,
                LocalDate.of(2026, 5, 16),
                LottoCombination.of(1, 2, 3, 4, 5, 6),
                7,
                2_000_000_000L,
                10,
                90_000_000_000L,
                20_000_000_000L,
                "{\"returnValue\":\"success\"}",
                null
        );
    }

    public static WinningNumber winningNumber(int round, LocalDate drawDate, LottoCombination combination, int bonusNumber) {
        return new WinningNumber(
                round,
                drawDate,
                combination,
                bonusNumber,
                2_000_000_000L,
                10,
                90_000_000_000L,
                20_000_000_000L,
                "{\"returnValue\":\"success\"}",
                null
        );
    }

    public static WinningNumberEntity winningNumberEntityFromDomain(WinningNumber winningNumber, LocalDateTime now) {
        return WinningNumberMapper.toEntity(winningNumber, now);
    }

    public static WinningNumberDto winningNumberDto(int round) {
        return new WinningNumberDto(
                round,
                LocalDate.of(2026, 5, 16),
                List.of(1, 2, 3, 4, 5, 6),
                7,
                2_000_000_000L,
                10,
                90_000_000_000L
        );
    }

    public static List<CombinationDto> combinationDtos(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(offset -> new CombinationDto(List.of(offset, offset + 10, offset + 20, 31, 40, 45)))
                .toList();
    }
}
