package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import java.util.List;
import java.util.Locale;

class NewsSourceClassifier {

    private final List<String> officialSources;
    private final List<String> pressSources;

    NewsSourceClassifier(List<String> officialSources, List<String> pressSources) {
        this.officialSources = officialSources == null ? List.of() : List.copyOf(officialSources);
        this.pressSources    = pressSources    == null ? List.of() : List.copyOf(pressSources);
    }

    NewsSourceTier classify(String source) {
        if (source == null || source.isBlank()) {
            return NewsSourceTier.GENERAL;
        }
        String lower = source.toLowerCase(Locale.KOREAN);
        for (String s : officialSources) {
            if (lower.contains(s.toLowerCase(Locale.KOREAN))) {
                return NewsSourceTier.OFFICIAL;
            }
        }
        for (String s : pressSources) {
            if (lower.contains(s.toLowerCase(Locale.KOREAN))) {
                return NewsSourceTier.PRESS;
            }
        }
        return NewsSourceTier.GENERAL;
    }
}
