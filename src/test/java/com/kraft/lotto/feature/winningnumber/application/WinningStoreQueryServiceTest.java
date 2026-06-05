package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningStoreRepository;
import com.kraft.lotto.feature.winningnumber.web.dto.WinningStoreDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WinningStoreQueryService")
class WinningStoreQueryServiceTest {

    @Mock
    WinningStoreRepository repository;

    @InjectMocks
    WinningStoreQueryService service;

    @Test
    @DisplayName("회차와 등수로 판매점 목록을 조회한다")
    void findByRoundAndGrade() {
        WinningStoreEntity entity = new WinningStoreEntity(1226, 1, "거봉마트", "대구 서구", 1, LocalDateTime.now());
        when(repository.findByRoundAndGradeOrderByIdAsc(1226, 1)).thenReturn(List.of(entity));

        List<WinningStoreDto> result = service.findByRoundAndGrade(1226, 1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("거봉마트");
        assertThat(result.get(0).grade()).isEqualTo(1);
    }

    @Test
    @DisplayName("판매점이 없으면 빈 목록을 반환한다")
    void findByRoundAndGradeEmpty() {
        when(repository.findByRoundAndGradeOrderByIdAsc(1226, 2)).thenReturn(List.of());

        assertThat(service.findByRoundAndGrade(1226, 2)).isEmpty();
    }

    @Test
    @DisplayName("해당 회차의 판매점 수집 여부를 확인한다")
    void hasStores() {
        when(repository.existsByRound(1226)).thenReturn(true);
        when(repository.existsByRound(1225)).thenReturn(false);

        assertThat(service.hasStores(1226)).isTrue();
        assertThat(service.hasStores(1225)).isFalse();
    }

    @Test
    @DisplayName("특정 등급의 판매점 수집 여부를 확인한다")
    void hasGrade() {
        when(repository.existsByRoundAndGrade(1226, 1)).thenReturn(true);
        when(repository.existsByRoundAndGrade(1226, 2)).thenReturn(false);

        assertThat(service.hasGrade(1226, 1)).isTrue();
        assertThat(service.hasGrade(1226, 2)).isFalse();
    }

    @Test
    @DisplayName("해당 회차의 마지막 수집 시각을 반환한다")
    void findLastCollectedAt() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 5, 12, 0);
        when(repository.findLastCollectedAtByRound(1226)).thenReturn(Optional.of(now));
        when(repository.findLastCollectedAtByRound(1225)).thenReturn(Optional.empty());

        assertThat(service.findLastCollectedAt(1226)).contains(now);
        assertThat(service.findLastCollectedAt(1225)).isEmpty();
    }
}
