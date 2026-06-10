package com.kraft.lotto.feature.saved.application;

import com.kraft.lotto.feature.saved.domain.SavedNumbersEntity;
import com.kraft.lotto.feature.saved.infrastructure.SavedNumbersRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SavedNumbersService {

    private static final int MAX_PER_DEVICE = 100;
    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 45;
    private static final int NUMBERS_COUNT = 6;

    private final SavedNumbersRepository repository;

    @Transactional(readOnly = true)
    public List<SavedNumbersDto> list(String deviceToken) {
        return repository.findByDeviceTokenOrderBySavedAtDesc(deviceToken)
                .stream()
                .map(SavedNumbersDto::from)
                .toList();
    }

    @Transactional
    public SavedNumbersDto save(String deviceToken, List<Integer> numbers, String label) {
        validateNumbers(numbers);
        if (repository.countByDeviceToken(deviceToken) >= MAX_PER_DEVICE) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(422),
                    "저장 가능한 번호 개수(" + MAX_PER_DEVICE + "개)를 초과했습니다.", null);
        }
        SavedNumbersEntity entity = new SavedNumbersEntity(deviceToken, numbers, label);
        return SavedNumbersDto.from(repository.save(entity));
    }

    @Transactional
    public void delete(Long id, String deviceToken) {
        repository.deleteByIdAndDeviceToken(id, deviceToken);
    }

    private void validateNumbers(List<Integer> numbers) {
        if (numbers == null || numbers.size() != NUMBERS_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "번호는 정확히 " + NUMBERS_COUNT + "개여야 합니다.", null);
        }
        long distinct = numbers.stream().distinct().count();
        if (distinct != NUMBERS_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "번호에 중복이 있습니다.", null);
        }
        for (int n : numbers) {
            if (n < MIN_NUMBER || n > MAX_NUMBER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "번호는 " + MIN_NUMBER + "~" + MAX_NUMBER + " 범위여야 합니다.", null);
            }
        }
    }

    public record SavedNumbersDto(Long id, List<Integer> numbers, String label, LocalDateTime savedAt) {
        static SavedNumbersDto from(SavedNumbersEntity e) {
            return new SavedNumbersDto(e.getId(), e.getNumbers(), e.getLabel(), e.getSavedAt());
        }
    }
}
