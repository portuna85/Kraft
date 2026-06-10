package com.kraft.lotto.web;

import java.util.List;

public final class SeoPages {

    public static final List<Page> ALL = List.of(
            new Page("/",                 "daily",   "1.0", true),
            new Page("/latest",           "daily",   "0.9", true),
            new Page("/frequency",        "weekly",  "0.8", true),
            new Page("/rounds",           "daily",   "0.8", true),
            new Page("/stats",            "weekly",  "0.7", true),
            new Page("/analysis",         "weekly",  "0.7", true),
            new Page("/companion",        "weekly",  "0.6", true),
            new Page("/news",             "daily",   "0.7", true),
            new Page("/methodology",      "monthly", "0.5", false),
            new Page("/data-source",      "monthly", "0.5", false),
            new Page("/faq",              "monthly", "0.5", false),
            new Page("/responsible-play", "monthly", "0.4", false),
            new Page("/privacy",          "yearly",  "0.3", false),
            new Page("/terms",            "yearly",  "0.3", false),
            new Page("/contact",          "monthly", "0.3", false)
    );

    private SeoPages() {}

    public record Page(String path, String changefreq, String priority, boolean dynamicLastmod) {}
}
