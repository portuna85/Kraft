package com.kraft.lotto.feature.news.web.dto;

import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record NewsArticleDto(
        Long id,
        String title,
        String link,
        String description,
        String source,
        String pubDateFormatted,
        NewsSourceTier tier
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static NewsArticleDto of(Long id, String title, String link,
                                     String description, String source,
                                     LocalDateTime pubDate, NewsSourceTier tier) {
        String formatted = pubDate != null ? pubDate.format(FMT) : "";
        return new NewsArticleDto(id, title, link, description, source, formatted, tier);
    }
}
