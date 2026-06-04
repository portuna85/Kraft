package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.domain.NewsArticle;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

public class NewsCollectionService {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectionService.class);

    private final NewsArticleRepository repository;
    private final NewsRssClient rssClient;
    private final NewsArticlePersister persister;
    private final NewsSourceClassifier classifier;
    private final Clock clock;
    private final int retentionDays;

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          Clock clock,
                          int retentionDays) {
        this(repository, rssClient, persister, new NewsSourceClassifier(List.of(), List.of()), clock, retentionDays);
    }

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          NewsSourceClassifier classifier,
                          Clock clock,
                          int retentionDays) {
        this.repository = repository;
        this.rssClient = rssClient;
        this.persister = persister;
        this.classifier = classifier;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    public NewsCollectResult collect() {
        List<NewsArticle> articles = rssClient.fetch();
        if (articles.isEmpty()) {
            return new NewsCollectResult(0, 0);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        int saved = 0;
        int skipped = 0;

        for (NewsArticle article : articles) {
            String hash = sha256(article.link());
            if (repository.existsByLinkHash(hash)) {
                skipped++;
                continue;
            }
            try {
                persister.saveArticle(new NewsArticleEntity(
                        truncate(article.title(), 500),
                        truncate(article.link(), 2000),
                        hash,
                        truncate(article.description(), 2000),
                        truncate(article.source(), 200),
                        classifier.classify(article.source()),
                        article.pubDate(),
                        now
                ));
                saved++;
            } catch (DataIntegrityViolationException e) {
                skipped++;
                log.warn("article duplicate, skipped: hash={}", hash);
            }
        }

        log.info("news collect done saved={} skipped={}", saved, skipped);
        return new NewsCollectResult(saved, skipped);
    }

    @Transactional
    public int purgeOldArticles() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        int deleted = repository.deleteByCollectedAtBefore(cutoff);
        if (deleted > 0) {
            log.info("news retention purged count={} cutoff={}", deleted, cutoff);
        }
        return deleted;
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    public record NewsCollectResult(int saved, int skipped) {
    }
}
