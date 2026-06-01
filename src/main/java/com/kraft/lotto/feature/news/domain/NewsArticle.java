package com.kraft.lotto.feature.news.domain;

import java.time.LocalDateTime;

public record NewsArticle(
        Long id,
        String title,
        String link,
        String description,
        String source,
        LocalDateTime pubDate,
        LocalDateTime collectedAt
) {
}
