package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.infrastructure.NewsArticleEntity;
import com.kraft.lotto.feature.news.infrastructure.NewsArticleRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class NewsArticlePersister {

    private final NewsArticleRepository repository;

    NewsArticlePersister(NewsArticleRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void saveArticle(NewsArticleEntity article) {
        repository.saveAndFlush(article);
    }
}
