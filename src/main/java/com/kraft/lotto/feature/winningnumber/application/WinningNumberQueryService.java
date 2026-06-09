package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.domain.LottoDrawSchedule;
import com.kraft.lotto.feature.winningnumber.domain.LottoRoundPolicy;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberMapper;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberDto;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningNumberPageDto;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WinningNumberQueryService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final WinningNumberRepository repository;
    private final Clock clock;

    public WinningNumberDto getLatest() {
        return repository.findTopByOrderByRoundDesc()
                .map(WinningNumberMapper::toDomain)
                .map(WinningNumberDto::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.WINNING_NUMBER_NOT_FOUND));
    }

    public Optional<WinningNumberDto> findLatest() {
        return repository.findTopByOrderByRoundDesc()
                .map(WinningNumberMapper::toDomain)
                .map(WinningNumberDto::from);
    }

    public WinningNumberDto getByRound(int round) {
        if (round < LottoRoundPolicy.MIN_ROUND || round > LottoRoundPolicy.maxCollectableRound(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_TARGET_ROUND);
        }
        return repository.findById(round)
                .map(WinningNumberMapper::toDomain)
                .map(WinningNumberDto::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.WINNING_NUMBER_NOT_FOUND));
    }

    public Optional<WinningNumberDto> findByRound(int round) {
        if (round < LottoRoundPolicy.MIN_ROUND || round > LottoRoundPolicy.maxCollectableRound(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.LOTTO_INVALID_TARGET_ROUND);
        }
        return repository.findById(round)
                .map(WinningNumberMapper::toDomain)
                .map(WinningNumberDto::from);
    }

    public int expectedCurrentRound() {
        return LottoDrawSchedule.expectedRound(LocalDate.now(clock));
    }

    public int maxPossibleRound() {
        return LottoRoundPolicy.maxCollectableRound(LocalDate.now(clock));
    }

    public int latestPersistedRound() {
        return repository.findMaxRound().orElse(0);
    }

    public int userSearchMaxRound() {
        int latest = latestPersistedRound();
        return latest > 0 ? latest : LottoDrawSchedule.expectedRound(LocalDate.now(clock));
    }

    public WinningNumberPageDto list(int page, int size) {
        int safePage = (int) Math.clamp(page, 0, Integer.MAX_VALUE);
        int safeSize = (int) Math.clamp(size, 1, MAX_PAGE_SIZE);
        var entities = repository.findAllByOrderByRoundDesc(PageRequest.of(safePage, safeSize));
        var mapped = entities
                .map(WinningNumberMapper::toDomain)
                .map(WinningNumberDto::from);
        return WinningNumberPageDto.from(mapped);
    }
}
