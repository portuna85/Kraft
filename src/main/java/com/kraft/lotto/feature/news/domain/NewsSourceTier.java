package com.kraft.lotto.feature.news.domain;

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
}
