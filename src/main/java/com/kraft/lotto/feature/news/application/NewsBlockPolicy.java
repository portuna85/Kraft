package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.domain.NewsArticle;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

class NewsBlockPolicy {

    private final List<String> configBlockedDomains;
    private final List<String> configBlockedKeywords;

    NewsBlockPolicy(List<String> configBlockedDomains, List<String> configBlockedKeywords) {
        this.configBlockedDomains = List.copyOf(configBlockedDomains);
        this.configBlockedKeywords = List.copyOf(configBlockedKeywords);
    }

    Optional<NewsRejectReason> evaluate(String domain, NewsArticle article,
                                        List<String> dbDomains, List<String> dbKeywords) {
        if (isDomainBlocked(domain, dbDomains)) {
            return Optional.of(NewsRejectReason.BLOCKED_DOMAIN);
        }
        if (isKeywordBlocked(article, dbKeywords)) {
            return Optional.of(NewsRejectReason.BLOCKED_KEYWORD);
        }
        return Optional.empty();
    }

    static String extractDomain(String link) {
        try {
            String host = URI.create(link).getHost();
            return host != null ? host.toLowerCase(Locale.ROOT) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isDomainBlocked(String domain, List<String> dbDomains) {
        if (domain == null) {
            return true;
        }
        return matchesDomainList(domain, configBlockedDomains)
                || matchesDomainList(domain, dbDomains);
    }

    private static boolean matchesDomainList(String domain, List<String> domains) {
        return domains.stream()
                .map(d -> d.toLowerCase(Locale.ROOT))
                .anyMatch(domain::endsWith);
    }

    private boolean isKeywordBlocked(NewsArticle article, List<String> dbKeywords) {
        String target = String.join(" ",
                nullToEmpty(article.title()),
                nullToEmpty(article.description()),
                nullToEmpty(article.source()),
                nullToEmpty(article.link())
        ).toLowerCase(Locale.KOREAN);
        return containsAny(target, configBlockedKeywords) || containsAny(target, dbKeywords);
    }

    private static boolean containsAny(String target, List<String> keywords) {
        return keywords.stream()
                .map(k -> k == null ? "" : k.trim().toLowerCase(Locale.KOREAN))
                .filter(k -> !k.isBlank())
                .anyMatch(target::contains);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
