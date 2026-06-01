package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningStoreDto;
import java.util.List;
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
}
