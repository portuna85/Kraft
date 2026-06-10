package com.kraft.lotto.feature.push.infrastructure;

import com.kraft.lotto.feature.push.domain.DeviceTokenEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, Long> {

    Optional<DeviceTokenEntity> findByToken(String token);

    @Modifying
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.token = :token")
    int deleteByToken(@Param("token") String token);

    @Query("SELECT d.token FROM DeviceTokenEntity d WHERE d.lastSeenAt < :cutoff")
    List<String> findStaleTokens(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM DeviceTokenEntity d WHERE d.lastSeenAt < :cutoff")
    int deleteStaleTokens(@Param("cutoff") LocalDateTime cutoff);
}
