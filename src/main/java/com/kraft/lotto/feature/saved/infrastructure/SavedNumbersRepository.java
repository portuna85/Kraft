package com.kraft.lotto.feature.saved.infrastructure;

import com.kraft.lotto.feature.saved.domain.SavedNumbersEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedNumbersRepository extends JpaRepository<SavedNumbersEntity, Long> {

    List<SavedNumbersEntity> findByDeviceTokenOrderBySavedAtDesc(String deviceToken);

    long countByDeviceToken(String deviceToken);

    void deleteByIdAndDeviceToken(Long id, String deviceToken);
}
