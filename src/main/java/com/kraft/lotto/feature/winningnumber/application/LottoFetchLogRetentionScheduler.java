package com.kraft.lotto.feature.winningnumber.application;

import com.kraft.lotto.feature.winningnumber.infrastructure.LottoFetchLogRepository;
import com.kraft.lotto.infra.config.KraftCollectProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
@ConditionalOnProperty(prefix = "kraft.collect.log-retention", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LottoFetchLogRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(LottoFetchLogRetentionScheduler.class);

    private final LottoFetchLogRepository fetchLogRepository;
    private final Clock clock;
    private final int retentionDays;
    private final int deleteBatchSize;

    @Autowired
    public LottoFetchLogRetentionScheduler(LottoFetchLogRepository fetchLogRepository,
                                           KraftCollectProperties collectProperties) {
        this(fetchLogRepository, Clock.systemDefaultZone(), collectProperties);
    }

    LottoFetchLogRetentionScheduler(LottoFetchLogRepository fetchLogRepository,
                                    Clock clock,
                                    KraftCollectProperties collectProperties) {
        this.fetchLogRepository = fetchLogRepository;
        this.clock = clock;
        this.retentionDays = Math.max(1, collectProperties.logRetention().days());
        this.deleteBatchSize = Math.max(100, collectProperties.logRetention().deleteBatchSize());
    }

    @Scheduled(
            cron = "${kraft.collect.log-retention.cron:0 30 3 * * *}",
            zone = "${kraft.collect.auto.zone:Asia/Seoul}"
    )
    public void purgeExpiredLogs() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        long deleted = 0L;
        while (true) {
            List<Long> ids = fetchLogRepository.findIdsByFetchedAtBefore(cutoff, PageRequest.of(0, deleteBatchSize));
            if (ids.isEmpty()) {
                break;
            }
            fetchLogRepository.deleteAllByIdInBatch(ids);
            deleted += ids.size();
            if (ids.size() == deleteBatchSize) {
                // 대량 삭제 시 배치 사이에 짧은 대기로 DB 부하 분산
                try { Thread.sleep(50); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); break; }
            }
        }
        if (deleted > 0) {
            log.info("lotto_fetch_logs retention purge done: deleted={}, cutoff={}", deleted, cutoff);
        }
    }
}
