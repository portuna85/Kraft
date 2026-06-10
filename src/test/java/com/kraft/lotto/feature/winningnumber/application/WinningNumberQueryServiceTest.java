package com.kraft.lotto.feature.winningnumber.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumber;
import static com.kraft.lotto.support.fixtures.LottoTestFixtures.winningNumberEntityFromDomain;

import com.kraft.lotto.feature.winningnumber.domain.LottoCombination;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberEntity;
import com.kraft.lotto.feature.winningnumber.infrastructure.WinningNumberRepository;
import com.kraft.lotto.support.BusinessException;
import com.kraft.lotto.support.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("당첨 번호 조회 서비스 테스트")
class WinningNumberQueryServiceTest {

    @Mock
    WinningNumberRepository repository;

    WinningNumberQueryService service;

    @BeforeEach
    void setUp() {
        service = new WinningNumberQueryService(repository, Clock.systemDefaultZone());
    }

    private static WinningNumberEntity entity(int round) {
        return winningNumberEntityFromDomain(
                winningNumber(round, LocalDate.of(2024, 1, 1).plusWeeks(round), LottoCombination.of(1, 7, 13, 22, 34, 45), 8),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("최신 당첨 번호를 반환한다")
    void getLatestReturnsLatest() {
        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(entity(1100)));

        var dto = service.getLatest();

        assertThat(dto.round()).isEqualTo(1100);
        assertThat(dto.numbers()).hasSize(6);
    }

    @Test
    @DisplayName("당첨 번호가 없을 때 최신 조회를 시도하면 예외가 발생한다")
    void getLatestThrowsNotFoundWhenAbsent() {
        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.empty());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(service::getLatest)
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.WINNING_NUMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회차 조회 시 예외가 발생한다")
    void getByRoundThrowsNotFoundWhenAbsent() {
        when(repository.findById(100)).thenReturn(Optional.empty());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.getByRound(100))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.WINNING_NUMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("회차가 양수가 아닐 경우 조회 시 예외가 발생한다")
    void getByRoundThrowsNotFoundWhenRoundIsNonPositive() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> service.getByRound(0))
                .extracting(BusinessException::getErrorCode)
                .isEqualTo(ErrorCode.LOTTO_INVALID_TARGET_ROUND);
    }

    @Test
    @DisplayName("목록 조회 결과를 디티오로 반환한다")
    void listReturnsMappedDto() {
        Page<WinningNumberEntity> page = new PageImpl<>(List.of(entity(2), entity(1)), PageRequest.of(0, 20), 2);
        when(repository.findAllByOrderByRoundDesc(any())).thenReturn(page);

        var result = service.list(0, 20);

        assertThat(result.content()).hasSize(2);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.page()).isZero();
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("최대 가능 회차는 현재 날짜 기준으로 양수를 반환한다")
    void maxPossibleRoundReturnsPositive() {
        int max = service.maxPossibleRound();

        assertThat(max).isPositive();
        assertThat(max).isGreaterThan(1200);
    }

    @Test
    @DisplayName("최신 조회는 당첨번호가 있으면 옵셔널을 반환한다")
    void findLatestReturnsOptionalWhenPresent() {
        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.of(entity(1100)));

        var result = service.findLatest();

        assertThat(result).isPresent();
        assertThat(result.get().round()).isEqualTo(1100);
    }

    @Test
    @DisplayName("최신 조회는 당첨번호가 없으면 빈 옵셔널을 반환한다")
    void findLatestReturnsEmptyWhenAbsent() {
        when(repository.findTopByOrderByRoundDesc()).thenReturn(Optional.empty());

        assertThat(service.findLatest()).isEmpty();
    }

    @Test
    @DisplayName("회차 조회는 유효한 회차에서 옵셔널을 반환한다")
    void findByRoundReturnsOptionalForValidRound() {
        when(repository.findById(100)).thenReturn(Optional.of(entity(100)));

        var result = service.findByRound(100);

        assertThat(result).isPresent();
        assertThat(result.get().round()).isEqualTo(100);
    }

    @Test
    @DisplayName("회차 조회는 해당 회차가 없으면 빈 옵셔널을 반환한다")
    void findByRoundReturnsEmptyWhenAbsent() {
        when(repository.findById(100)).thenReturn(Optional.empty());

        assertThat(service.findByRound(100)).isEmpty();
    }

    @Test
    @DisplayName("예상 현재 회차는 양수를 반환한다")
    void expectedCurrentRoundReturnsPositive() {
        assertThat(service.expectedCurrentRound()).isPositive();
    }
}
