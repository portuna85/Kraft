package com.kraft.winningnumber;

import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WinningNumberCommandService {

    private final WinningNumberRepository winningNumberRepository;
    private final LottoNumberCodec lottoNumberCodec;
    private final Clock clock;
    private final WinningNumberInsertExecutor insertExecutor;
    private final Validator validator;

    public WinningNumberCommandService(WinningNumberRepository winningNumberRepository,
                                       LottoNumberCodec lottoNumberCodec,
                                       Clock clock,
                                       WinningNumberInsertExecutor insertExecutor,
                                       Validator validator) {
        this.winningNumberRepository = winningNumberRepository;
        this.lottoNumberCodec = lottoNumberCodec;
        this.clock = clock;
        this.insertExecutor = insertExecutor;
        this.validator = validator;
    }

    public WinningNumberResponse upsert(WinningNumberUpsertRequest request) {
        return upsertWithResult(request).response();
    }

    /**
     * 컨트롤러의 {@code @Valid}는 이 메서드를 직접 호출하는 외부 수집 경로(스케줄러·백필)에는
     * 적용되지 않으므로, 그 경로에서 유일하게 공유되는 이 지점에서 프로그램적으로 재검증한다.
     * 관리자 콘솔 수동 upsert는 컨트롤러의 @Valid가 먼저 걸러내므로 위반이 발생하지 않는다.
     */
    public WinningNumberUpsertResult upsertWithResult(WinningNumberUpsertRequest request) {
        Set<ConstraintViolation<WinningNumberUpsertRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_VALIDATION_ERROR", summarize(violations));
        }
        if (request.drawDate().isAfter(LocalDate.now(clock).plusDays(1))) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LOTTO_SOURCE_INVALID_DATE",
                    "추첨일이 미래입니다: " + request.drawDate());
        }

        var normalized = lottoNumberCodec.normalize(request.numbers());
        if (normalized.contains(request.bonusNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BONUS_NUMBER",
                    "보너스 번호는 당첨 번호 6개와 중복될 수 없습니다.");
        }

        var existing = winningNumberRepository.findByRound(request.round());
        if (existing.isPresent()) {
            return applyUpdate(existing.get(), request, normalized);
        }

        try {
            WinningNumberResponse response = insertExecutor.insertNew(createNew(request, normalized));
            return new WinningNumberUpsertResult(response, true);
        } catch (DataIntegrityViolationException ex) {
            // 자동 수집 스케줄러와 수동 upsert가 같은 신규 회차를 거의 동시에 처리하면 uk_winning_numbers_round
            // 유니크 제약 위반으로 여기에 도달한다. insertExecutor가 REQUIRES_NEW로 분리돼 있어 그
            // 트랜잭션만 롤백되고(JPA 스펙상 제약 위반 후 영속성 컨텍스트는 더 이상 사용할 수 없다),
            // 현재(이 메서드의) 트랜잭션은 깨끗한 상태로 동시 승자가 이미 insert한 행을 update로 재해석한다.
            WinningNumber concurrent = winningNumberRepository.findByRound(request.round()).orElseThrow(() -> ex);
            return applyUpdate(concurrent, request, normalized);
        }
    }

    private WinningNumberUpsertResult applyUpdate(WinningNumber existing, WinningNumberUpsertRequest request,
                                                   java.util.List<Integer> normalized) {
        boolean changed = hasChanges(existing, request, normalized);
        if (changed) {
            updateExisting(existing, request, normalized);
        }
        WinningNumberResponse response = WinningNumberResponse.from(winningNumberRepository.save(existing));
        return new WinningNumberUpsertResult(response, changed);
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
                || orZero(existing.getSecondPrize()) != orZero(request.secondPrize())
                || orZeroInt(existing.getSecondWinners()) != orZeroInt(request.secondWinners())
                || orZero(existing.getTotalSales()) != orZero(request.totalSales())
                || orZero(existing.getFirstAccumAmount()) != orZero(request.firstAccumAmount());
    }

    private void updateExisting(WinningNumber existing,
                                WinningNumberUpsertRequest request,
                                java.util.List<Integer> normalized) {
        fieldsFrom(request, normalized).applyUpdateTo(existing);
    }

    private WinningNumber createNew(WinningNumberUpsertRequest request, java.util.List<Integer> normalized) {
        return fieldsFrom(request, normalized)
                .round(request.round())
                .createdAt(OffsetDateTime.now(clock))
                .build();
    }

    private WinningNumber.Builder fieldsFrom(WinningNumberUpsertRequest request, java.util.List<Integer> normalized) {
        return WinningNumber.builder()
                .drawDate(request.drawDate())
                .numbers(normalized.get(0), normalized.get(1), normalized.get(2),
                        normalized.get(3), normalized.get(4), normalized.get(5))
                .bonusNumber(request.bonusNumber())
                .firstPrizeAmount(request.firstPrizeAmount())
                .secondPrize(orZero(request.secondPrize()))
                .secondWinners(orZeroInt(request.secondWinners()))
                .totalSales(orZero(request.totalSales()))
                .firstAccumAmount(orZero(request.firstAccumAmount()));
    }

    private static String summarize(Set<ConstraintViolation<WinningNumberUpsertRequest>> violations) {
        return violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
    }

    private static long orZero(Long value) {
        return value != null ? value : 0L;
    }

    private static int orZeroInt(Integer value) {
        return value != null ? value : 0;
    }
}
