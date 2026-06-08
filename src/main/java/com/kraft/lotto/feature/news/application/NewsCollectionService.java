package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedDomainRepository;
import com.kraft.lotto.feature.admin.infrastructure.NewsBlockedKeywordRepository;
import com.kraft.lotto.feature.news.domain.NewsArticle;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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
    private final List<String> blockedDomains;
    private final List<String> blockedKeywords;
    private final NewsBlockedDomainRepository blockedDomainRepository;
    private final NewsBlockedKeywordRepository blockedKeywordRepository;

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

    private final NewsRelevancePolicy relevancePolicy;

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
        this.repository = repository;
        this.rssClient = rssClient;
        this.persister = persister;
        this.classifier = classifier;
        this.clock = clock;
        this.retentionDays = retentionDays;
        this.blockedDomains = List.copyOf(blockedDomains);
        this.blockedKeywords = List.copyOf(blockedKeywords);
        this.blockedDomainRepository = blockedDomainRepository;
        this.blockedKeywordRepository = blockedKeywordRepository;
        this.relevancePolicy = relevancePolicy;
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
            String domain = extractDomain(article.link());
            if (isBlockedDomain(domain, dbBlockedDomains)) {
                skipped++;
                log.debug("news blocked domain, skipped: link={}", article.link());
                continue;
            }
            if (isBlockedKeyword(article, dbBlockedKeywords)) {
                skipped++;
                log.debug("news blocked keyword, skipped: title={} link={}", article.title(), article.link());
                continue;
            }
            NewsDecision decision = relevancePolicy.decide(
                    article.title(), article.description(), article.source(), article.link());
            if (decision.type() == NewsDecision.Type.REJECT) {
                skipped++;
                log.debug("news relevance reject score={} title={}", decision.score(), article.title());
                continue;
            }
            String hash = sha256(article.link());
            if (repository.existsByLinkHash(hash)) {
                skipped++;
                continue;
            }
            try {
                var sourceTier = classifier.classify(article.source());
                var entity = new NewsArticleEntity(
                        truncate(article.title(), 500),
                        truncate(article.link(), 2000),
                        hash,
                        truncate(article.description(), 2000),
                        truncate(article.source(), 200),
                        domain,
                        sourceTier,
                        article.pubDate(),
                        now
                );
                if (decision.type() == NewsDecision.Type.REVIEW) {
                    entity.setApproved(false);
                }
                persister.saveArticle(entity);
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

    private String extractDomain(String link) {
        try {
            String host = URI.create(link).getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isBlockedDomain(String domain, List<String> dbDomains) {
        if (domain == null) {
            return true;
        }
        boolean blockedByConfig = blockedDomains.stream()
                .map(d -> d.toLowerCase(Locale.ROOT))
                .anyMatch(domain::endsWith);
        if (blockedByConfig) {
            return true;
        }
        return dbDomains.stream()
                .map(d -> d.toLowerCase(Locale.ROOT))
                .anyMatch(domain::endsWith);
    }

    private boolean isBlockedKeyword(NewsArticle article, List<String> dbKeywords) {
        String target = String.join(" ",
                nullToEmpty(article.title()),
                nullToEmpty(article.description()),
                nullToEmpty(article.source()),
                nullToEmpty(article.link())
        ).toLowerCase(Locale.KOREAN);

        return containsBlockedKeyword(target, blockedKeywords)
                || containsBlockedKeyword(target, dbKeywords);
    }

    private static boolean containsBlockedKeyword(String target, List<String> keywords) {
        return keywords.stream()
                .map(keyword -> keyword == null ? "" : keyword.trim().toLowerCase(Locale.KOREAN))
                .filter(keyword -> !keyword.isBlank())
                .anyMatch(target::contains);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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
