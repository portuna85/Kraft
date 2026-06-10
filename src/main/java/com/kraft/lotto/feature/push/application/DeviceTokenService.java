package com.kraft.lotto.feature.push.application;

import com.kraft.lotto.feature.push.domain.DeviceTokenEntity;
import com.kraft.lotto.feature.push.domain.Platform;
import com.kraft.lotto.feature.push.infrastructure.DeviceTokenRepository;
import com.kraft.lotto.infra.fcm.FcmService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);
    // Tokens not seen for 90 days are considered stale
    private static final int STALE_DAYS = 90;

    private final DeviceTokenRepository repository;
    private final FcmService fcmService;

    @Transactional
    public void register(String token, Platform platform) {
        Optional<DeviceTokenEntity> existing = repository.findByToken(token);
        if (existing.isPresent()) {
            existing.get().refreshLastSeen(LocalDateTime.now());
            log.debug("device token refreshed platform={}", platform);
        } else {
            repository.save(new DeviceTokenEntity(token, platform, LocalDateTime.now()));
            fcmService.subscribeToDrawResult(token);
            log.info("device token registered platform={}", platform);
        }
    }

    @Transactional
    public void unregister(String token) {
        int deleted = repository.deleteByToken(token);
        if (deleted > 0) {
            fcmService.unsubscribeFromDrawResult(token);
            log.info("device token unregistered");
        }
    }

    @Scheduled(cron = "${kraft.fcm.stale-cleanup-cron:0 0 4 * * *}", zone = "Asia/Seoul")
    @SchedulerLock(name = "push-stale-token-cleanup", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void cleanupStaleTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(STALE_DAYS);
        List<String> stale = repository.findStaleTokens(cutoff);
        if (stale.isEmpty()) {
            return;
        }
        fcmService.unsubscribeBatch(stale);
        int deleted = repository.deleteStaleTokens(cutoff);
        log.info("push stale token cleanup deleted={}", deleted);
    }
}
