package com.kraft.lotto.feature.news.application;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${kraft.lotto.scheduler.enabled:true} and ${kraft.news.enabled:true}")
class NewsCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectionScheduler.class);

    private final NewsCollectionService collectionService;

    NewsCollectionScheduler(NewsCollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @Scheduled(cron = "${kraft.news.cron:0 0 */6 * * *}", zone = "Asia/Seoul")
    @SchedulerLock(name = "news-collect", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    public void collect() {
        log.info("news collect start");
        try {
            NewsCollectionService.NewsCollectResult result = collectionService.collect();
            collectionService.purgeOldArticles();
            log.info("news collect done saved={} skipped={}", result.saved(), result.skipped());
        } catch (Exception e) {
            log.error("news collect failed error={}", e.getMessage(), e);
        }
    }
}
