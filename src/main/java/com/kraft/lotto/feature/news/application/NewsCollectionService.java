package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedDomainRepository;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedKeywordRepository;
import com.kraft.lotto.feature.news.domain.NewsArticle;
import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    private final NewsRelevancePolicy relevancePolicy;
    private final NewsBlockPolicy blockPolicy;
    private final NewsDuplicatePolicy duplicatePolicy;
    private final Clock clock;
    private final int retentionDays;
    private final NewsBlockedDomainRepository blockedDomainRepository;
    private final NewsBlockedKeywordRepository blockedKeywordRepository;
    private final MeterRegistry meterRegistry;

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          Clock clock,
                          int retentionDays) {
        this(repository, rssClient, persister, new NewsSourceClassifier(List.of(), List.of()),
                clock, retentionDays, List.of(), List.of(), null, null);
    }

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          NewsSourceClassifier classifier,
                          Clock clock,
                          int retentionDays,
                          List<String> blockedDomains) {
        this(repository, rssClient, persister, classifier, clock, retentionDays, blockedDomains, List.of(), null, null);
    }

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          NewsSourceClassifier classifier,
                          Clock clock,
                          int retentionDays,
                          List<String> blockedDomains,
                          NewsBlockedDomainRepository blockedDomainRepository) {
        this(repository, rssClient, persister, classifier, clock, retentionDays, blockedDomains, List.of(),
                blockedDomainRepository, null);
    }

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          NewsSourceClassifier classifier,
                          Clock clock,
                          int retentionDays,
                          List<String> blockedDomains,
                          List<String> blockedKeywords,
                          NewsBlockedDomainRepository blockedDomainRepository,
                          NewsBlockedKeywordRepository blockedKeywordRepository) {
        this(repository, rssClient, persister, classifier, clock, retentionDays,
                blockedDomains, blockedKeywords, blockedDomainRepository, blockedKeywordRepository,
                new NewsRelevancePolicy());
    }

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          NewsSourceClassifier classifier,
                          Clock clock,
                          int retentionDays,
                          List<String> blockedDomains,
                          List<String> blockedKeywords,
                          NewsBlockedDomainRepository blockedDomainRepository,
                          NewsBlockedKeywordRepository blockedKeywordRepository,
                          NewsRelevancePolicy relevancePolicy) {
        this(repository, rssClient, persister, classifier, clock, retentionDays,
                blockedDomains, blockedKeywords, blockedDomainRepository, blockedKeywordRepository,
                relevancePolicy, new SimpleMeterRegistry());
    }

    NewsCollectionService(NewsArticleRepository repository,
                          NewsRssClient rssClient,
                          NewsArticlePersister persister,
                          NewsSourceClassifier classifier,
                          Clock clock,
                          int retentionDays,
                          List<String> blockedDomains,
                          List<String> blockedKeywords,
                          NewsBlockedDomainRepository blockedDomainRepository,
                          NewsBlockedKeywordRepository blockedKeywordRepository,
                          NewsRelevancePolicy relevancePolicy,
                          MeterRegistry meterRegistry) {
        this.repository = repository;
        this.rssClient = rssClient;
        this.persister = persister;
        this.classifier = classifier;
        this.relevancePolicy = relevancePolicy;
        this.blockPolicy = new NewsBlockPolicy(blockedDomains, blockedKeywords);
        this.duplicatePolicy = new NewsDuplicatePolicy(repository);
        this.clock = clock;
        this.retentionDays = retentionDays;
        this.blockedDomainRepository = blockedDomainRepository;
        this.blockedKeywordRepository = blockedKeywordRepository;
        this.meterRegistry = meterRegistry;
    }

    public NewsCollectResult collect() {
        List<NewsArticle> articles = rssClient.fetch();
        if (articles.isEmpty()) {
            return new NewsCollectResult(0, 0);
        }

        List<String> dbBlockedDomains = blockedDomainRepository != null
                ? blockedDomainRepository.findAllDomains()
                : List.of();
        List<String> dbBlockedKeywords = blockedKeywordRepository != null
                ? blockedKeywordRepository.findAllKeywords()
                : List.of();

        LocalDateTime now = LocalDateTime.now(clock);
        int saved = 0;
        int skipped = 0;

        for (NewsArticle article : articles) {
            String domain = NewsBlockPolicy.extractDomain(article.link());

            Optional<NewsRejectReason> blockReject = blockPolicy.evaluate(domain, article, dbBlockedDomains, dbBlockedKeywords);
            if (blockReject.isPresent()) {
                log.debug("news blocked reason={} title={}", blockReject.get(), article.title());
                countSkipped(blockReject.get().name());
                skipped++;
                continue;
            }

            NewsDecision decision = relevancePolicy.decide(
                    article.title(), article.description(), article.source(), article.link());
            if (decision.type() == NewsDecision.Type.REJECT) {
                log.debug("news relevance reject score={} title={}", decision.score(), article.title());
                countSkipped(NewsRejectReason.RELEVANCE_REJECT.name());
                skipped++;
                continue;
            }

            String linkHash = sha256(article.link());
            String titleHash = sha256(normalizeTitle(article.title()));
            Optional<NewsRejectReason> dupReject = duplicatePolicy.evaluate(linkHash, titleHash, now.minusDays(7));
            if (dupReject.isPresent()) {
                log.debug("news duplicate skipped reason={} title={}", dupReject.get(), article.title());
                countSkipped(dupReject.get().name());
                countDuplicate(dupReject.get() == NewsRejectReason.LINK_DUPLICATE ? "link_hash" : "title_hash");
                skipped++;
                continue;
            }

            NewsSourceTier tier = classifier.classify(article.source());
            try {
                var entity = new NewsArticleEntity(
                        truncate(article.title(), 500),
                        truncate(article.link(), 2000),
                        linkHash,
                        truncate(article.description(), 2000),
                        truncate(article.source(), 200),
                        domain,
                        tier,
                        article.pubDate(),
                        now
                );
                if (decision.type() == NewsDecision.Type.REVIEW) {
                    entity.setApproved(false);
                }
                persister.saveArticle(entity);
                meterRegistry.counter("kraft.news.collect.saved", "sourceTier", tier.name()).increment();
                saved++;
            } catch (DataIntegrityViolationException e) {
                countSkipped("DB_DUPLICATE");
                skipped++;
                log.warn("article duplicate, skipped: link={}", article.link());
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

    private void countSkipped(String reason) {
        meterRegistry.counter("kraft.news.collect.skipped", "reason", reason).increment();
    }

    private void countDuplicate(String type) {
        meterRegistry.counter("kraft.news.duplicate", "type", type).increment();
    }

    private static String normalizeTitle(String title) {
        if (title == null) return "";
        return title.replaceAll("\\s+", " ")
                    .replaceAll("[\\[\\]【】()（）<>]", "")
                    .trim()
                    .toLowerCase(Locale.KOREAN);
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
