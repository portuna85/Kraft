package com.kraft.saved;

import com.kraft.common.config.SavedProperties;
import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import com.kraft.common.lotto.LottoRank;
import com.kraft.winningnumber.WinningNumberQueryService;
import com.kraft.winningnumber.WinningNumberResponse;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedNumbersService {

    private final SavedNumberRepository savedNumberRepository;
    private final LottoNumberCodec lottoNumberCodec;
    private final SavedProperties savedProperties;
    private final WinningNumberQueryService winningNumberQueryService;
    private final Clock clock;

    public SavedNumbersService(SavedNumberRepository savedNumberRepository,
                               LottoNumberCodec lottoNumberCodec,
                               SavedProperties savedProperties,
                               WinningNumberQueryService winningNumberQueryService,
                               Clock clock) {
        this.savedNumberRepository = savedNumberRepository;
        this.lottoNumberCodec = lottoNumberCodec;
        this.savedProperties = savedProperties;
        this.winningNumberQueryService = winningNumberQueryService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SavedNumberResponse> list(String clientTokenHash) {
        return savedNumberRepository.findByClientTokenHashOrderByCreatedAtDesc(clientTokenHash).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SavedNumberMatchResult> compareWithRound(String clientTokenHash, String roundParam) {
        WinningNumberResponse draw = "latest".equalsIgnoreCase(roundParam)
                ? winningNumberQueryService.findLatest()
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROUND_NOT_FOUND", "집계된 회차가 없습니다."))
                : winningNumberQueryService.getByRound(parseRound(roundParam));

        return savedNumberRepository.findByClientTokenHashOrderByCreatedAtDesc(clientTokenHash).stream()
                .map(saved -> toMatchResult(saved, draw))
                .toList();
    }

    public SaveNumberResult save(String clientTokenHash, CreateSavedNumberRequest request) {
        String normalizedNumbers = lottoNumberCodec.toStorageValue(request.numbers());
        return savedNumberRepository.findByClientTokenHashAndNumbers(clientTokenHash, normalizedNumbers)
                .map(existing -> new SaveNumberResult(toResponse(existing), false))
                .orElseGet(() -> createSavedNumber(clientTokenHash, request, normalizedNumbers));
    }

    public void delete(String clientTokenHash, long id) {
        SavedNumber savedNumber = savedNumberRepository.findByIdAndClientTokenHash(id, clientTokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SAVED_NUMBER_NOT_FOUND", "저장된 번호를 찾을 수 없습니다."));
        savedNumberRepository.delete(savedNumber);
    }

    private SaveNumberResult createSavedNumber(String clientTokenHash,
                                               CreateSavedNumberRequest request,
                                               String normalizedNumbers) {
        long currentCount = savedNumberRepository.countByClientTokenHash(clientTokenHash);
        if (currentCount >= savedProperties.maxPerClient()) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVED_LIMIT_REACHED", "이 기기에서 저장 가능한 번호 개수를 초과했습니다.");
        }

        String source = request.source() == null || request.source().isBlank() ? "MANUAL" : request.source().trim();
        String label = request.label() == null || request.label().isBlank() ? null : request.label().trim();
        try {
            SavedNumber savedNumber = savedNumberRepository.save(new SavedNumber(
                    clientTokenHash,
                    normalizedNumbers,
                    label,
                    source,
                    OffsetDateTime.now(clock)
            ));
            return new SaveNumberResult(toResponse(savedNumber), true);
        } catch (DataIntegrityViolationException ex) {
            // 동시 저장 레이스로 unique 제약 위반 시 기존 행을 반환하여 멱등 처리
            return savedNumberRepository.findByClientTokenHashAndNumbers(clientTokenHash, normalizedNumbers)
                    .map(existing -> new SaveNumberResult(toResponse(existing), false))
                    .orElseThrow(() -> ex);
        }
    }

    private static int parseRound(String roundParam) {
        try {
            int round = Integer.parseInt(roundParam);
            if (round < 1) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROUND", "round 파라미터가 올바르지 않습니다.");
            }
            return round;
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROUND", "round 파라미터가 올바르지 않습니다.");
        }
    }

    private SavedNumberMatchResult toMatchResult(SavedNumber saved, WinningNumberResponse draw) {
        List<Integer> savedNumbers = lottoNumberCodec.fromStorageValue(saved.getNumbers());
        Set<Integer> drawSet = new HashSet<>(draw.numbers());
        int matchedCount = (int) savedNumbers.stream().filter(drawSet::contains).count();
        boolean bonusMatch = savedNumbers.contains(draw.bonusNumber());
        return new SavedNumberMatchResult(
                toResponse(saved),
                draw.round(),
                draw.drawDate(),
                draw.numbers(),
                draw.bonusNumber(),
                matchedCount,
                bonusMatch,
                LottoRank.of(matchedCount, bonusMatch)
        );
    }

    private SavedNumberResponse toResponse(SavedNumber savedNumber) {
        return new SavedNumberResponse(
                savedNumber.getId(),
                lottoNumberCodec.fromStorageValue(savedNumber.getNumbers()),
                savedNumber.getLabel(),
                savedNumber.getSource(),
                savedNumber.getCreatedAt()
        );
    }
}
