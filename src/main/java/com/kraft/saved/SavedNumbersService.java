package com.kraft.saved;

import com.kraft.common.config.SavedProperties;
import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import com.kraft.common.lotto.LottoRank;
import com.kraft.winningnumber.WinningNumberQueryService;
import com.kraft.winningnumber.WinningNumberResponse;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedNumbersService {

    private final SavedNumberRepository savedNumberRepository;
    private final SavedNumberClientLockRepository savedNumberClientLockRepository;
    private final SavedNumberClientLockInitializer savedNumberClientLockInitializer;
    private final LottoNumberCodec lottoNumberCodec;
    private final SavedProperties savedProperties;
    private final WinningNumberQueryService winningNumberQueryService;
    private final Clock clock;

    public SavedNumbersService(SavedNumberRepository savedNumberRepository,
                               SavedNumberClientLockRepository savedNumberClientLockRepository,
                               SavedNumberClientLockInitializer savedNumberClientLockInitializer,
                               LottoNumberCodec lottoNumberCodec,
                               SavedProperties savedProperties,
                               WinningNumberQueryService winningNumberQueryService,
                               Clock clock) {
        this.savedNumberRepository = savedNumberRepository;
        this.savedNumberClientLockRepository = savedNumberClientLockRepository;
        this.savedNumberClientLockInitializer = savedNumberClientLockInitializer;
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

        // 클라이언트별 잠금 행을 확보(REQUIRES_NEW, 즉시 커밋)한 뒤 그 행을 레코드 락으로
        // 잠그고서 중복·한도 확인과 삽입까지 한 트랜잭션 안에서 마친다(B2). saved_numbers
        // 자체에 FOR UPDATE 범위 잠금을 걸면 아직 행이 없는 신규 클라이언트 구간에 갭 락이
        // 걸려 동시 INSERT끼리 데드락이 날 수 있어(2세션만으로도 재현됨), 항상 존재가
        // 보장되는 잠금 행을 레코드 락으로 잠그는 방식으로 바꿨다.
        savedNumberClientLockInitializer.ensureExists(clientTokenHash);
        savedNumberClientLockRepository.lockByClientTokenHash(clientTokenHash);

        List<SavedNumber> existing = savedNumberRepository.findByClientTokenHashOrderByCreatedAtDesc(clientTokenHash);

        Optional<SavedNumber> duplicate = existing.stream()
                .filter(saved -> saved.getNumbers().equals(normalizedNumbers))
                .findFirst();
        if (duplicate.isPresent()) {
            return new SaveNumberResult(toResponse(duplicate.get()), false);
        }

        if (existing.size() >= savedProperties.maxPerClient()) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVED_LIMIT_REACHED", "이 기기에서 저장 가능한 번호 개수를 초과했습니다.");
        }

        String source = request.source() == null || request.source().isBlank() ? "MANUAL" : request.source().trim();
        String label = request.label() == null || request.label().isBlank() ? null : request.label().trim();
        SavedNumber savedNumber = savedNumberRepository.save(new SavedNumber(
                clientTokenHash,
                normalizedNumbers,
                label,
                source,
                OffsetDateTime.now(clock)
        ));
        return new SaveNumberResult(toResponse(savedNumber), true);
    }

    public void delete(String clientTokenHash, long id) {
        SavedNumber savedNumber = savedNumberRepository.findByIdAndClientTokenHash(id, clientTokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SAVED_NUMBER_NOT_FOUND", "저장된 번호를 찾을 수 없습니다."));
        savedNumberRepository.delete(savedNumber);
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
