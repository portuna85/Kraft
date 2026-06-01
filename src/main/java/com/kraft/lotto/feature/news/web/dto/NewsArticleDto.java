package com.kraft.lotto.feature.news.web.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record NewsArticleDto(
        Long id,
        String title,
        String link,
        String description,
        String source,
        String pubDateFormatted
) {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public static NewsArticleDto of(Long id, String title, String link,
                                     String description, String source, LocalDateTime pubDate) {
        String formatted = pubDate != null ? pubDate.format(FMT) : "";
        return new NewsArticleDto(id, title, link, description, source, formatted);
    }
}
