package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import java.time.LocalDateTime;
import java.util.Optional;

class NewsDuplicatePolicy {

    private final NewsArticleRepository repository;

    NewsDuplicatePolicy(NewsArticleRepository repository) {
        this.repository = repository;
    }

    Optional<NewsRejectReason> evaluate(String linkHash, String normalizedTitleHash, LocalDateTime titleDupWindow) {
        if (repository.existsByLinkHash(linkHash)) {
            return Optional.of(NewsRejectReason.LINK_DUPLICATE);
        }
        if (repository.existsByTitleHashAndCollectedAtAfter(normalizedTitleHash, titleDupWindow)) {
            return Optional.of(NewsRejectReason.TITLE_DUPLICATE);
        }
        return Optional.empty();
    }
}
