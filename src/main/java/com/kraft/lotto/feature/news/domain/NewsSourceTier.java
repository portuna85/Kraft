package com.kraft.lotto.feature.news.domain;

import java.util.Locale;
import java.util.Optional;

public enum NewsSourceTier {

    OFFICIAL("공식", "tier-official"),
    PRESS("언론", "tier-press"),
    GENERAL("일반", "tier-general");

    private final String label;
    private final String cssClass;

    NewsSourceTier(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String label() { return label; }
    public String cssClass() { return cssClass; }

    public String paramValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<NewsSourceTier> fromParam(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (NewsSourceTier tier : values()) {
            if (tier.name().equals(normalized)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }
}
