package com.kraft.lotto.feature.admin.web;

import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/ops/seo")
public class AdminSeoController {

    private static final String DEFAULT_BASE_URL = "https://www.kraft.io.kr";

    private final Environment environment;

    public AdminSeoController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping
    public String seoPage(Model model) {
        String baseUrl = environment.getProperty("kraft.public-base-url", DEFAULT_BASE_URL);

        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("sitemapUrl", baseUrl + "/sitemap.xml");
        model.addAttribute("robotsTxtUrl", baseUrl + "/robots.txt");
        model.addAttribute("robotsDisallowed", List.of("/admin", "/ops", "/actuator", "/fragments/"));
        model.addAttribute("noindexPaths", List.of("/admin/**", "/ops/**", "/actuator/**"));
        model.addAttribute("sitemapEntries", buildSitemapEntries(baseUrl));

        return "admin/seo";
    }

    private static List<SitemapEntry> buildSitemapEntries(String baseUrl) {
        return List.of(
                new SitemapEntry(baseUrl + "/",                  "daily",   "1.0"),
                new SitemapEntry(baseUrl + "/frequency",         "weekly",  "0.8"),
                new SitemapEntry(baseUrl + "/rounds",            "daily",   "0.8"),
                new SitemapEntry(baseUrl + "/stats",             "weekly",  "0.7"),
                new SitemapEntry(baseUrl + "/analysis",          "weekly",  "0.7"),
                new SitemapEntry(baseUrl + "/companion",         "weekly",  "0.6"),
                new SitemapEntry(baseUrl + "/news",              "daily",   "0.7"),
                new SitemapEntry(baseUrl + "/news?tier=official","daily",   "0.6"),
                new SitemapEntry(baseUrl + "/news?tier=press",   "daily",   "0.6"),
                new SitemapEntry(baseUrl + "/methodology",       "monthly", "0.5"),
                new SitemapEntry(baseUrl + "/data-source",       "monthly", "0.5"),
                new SitemapEntry(baseUrl + "/faq",               "monthly", "0.5"),
                new SitemapEntry(baseUrl + "/responsible-play",  "monthly", "0.4"),
                new SitemapEntry(baseUrl + "/privacy",           "yearly",  "0.3"),
                new SitemapEntry(baseUrl + "/terms",             "yearly",  "0.3"),
                new SitemapEntry(baseUrl + "/contact",           "monthly", "0.3")
        );
    }

    public record SitemapEntry(String url, String changefreq, String priority) {}
}
