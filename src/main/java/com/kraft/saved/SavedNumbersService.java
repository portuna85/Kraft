package com.kraft.saved;

import com.kraft.common.config.SavedProperties;
import com.kraft.common.error.ApiException;
import com.kraft.common.lotto.LottoNumberCodec;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SavedNumbersService {

    private final SavedNumberRepository savedNumberRepository;
    private final LottoNumberCodec lottoNumberCodec;
    private final SavedProperties savedProperties;
    private final Clock clock;

    public SavedNumbersService(SavedNumberRepository savedNumberRepository,
                               LottoNumberCodec lottoNumberCodec,
                               SavedProperties savedProperties,
                               Clock clock) {
        this.savedNumberRepository = savedNumberRepository;
        this.lottoNumberCodec = lottoNumberCodec;
        this.savedProperties = savedProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SavedNumberResponse> list(String clientTokenHash) {
        return savedNumberRepository.findByClientTokenHashOrderByCreatedAtDesc(clientTokenHash).stream()
                .map(this::toResponse)
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
        SavedNumber savedNumber = savedNumberRepository.save(new SavedNumber(
                clientTokenHash,
                normalizedNumbers,
                label,
                source,
                OffsetDateTime.now(clock)
        ));
        return new SaveNumberResult(toResponse(savedNumber), true);
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
