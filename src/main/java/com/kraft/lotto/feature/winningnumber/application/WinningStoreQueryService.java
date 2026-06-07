package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningRegionSummaryDto;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningStoreDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WinningStoreQueryService {

    private final WinningStoreRepository repository;

    public List<WinningStoreDto> findByRoundAndGrade(int round, int grade) {
        return repository.findByRoundAndGradeOrderByIdAsc(round, grade)
                .stream()
                .map(e -> WinningStoreDto.from(e.toDomain()))
                .toList();
    }

    public boolean hasStores(int round) {
        return repository.existsByRound(round);
    }

    public boolean hasGrade(int round, int grade) {
        return repository.existsByRoundAndGrade(round, grade);
    }

    public Optional<LocalDateTime> findLastCollectedAt(int round) {
        return repository.findLastCollectedAtByRound(round);
    }

    public List<WinningRegionSummaryDto> findRegionSummary(int round) {
        return repository.findRegionSummaryByRound(round).stream()
                .map(r -> new WinningRegionSummaryDto(round, r.getGrade(), r.getSido(), r.getSigungu(), r.getCount()))
                .toList();
    }
}
