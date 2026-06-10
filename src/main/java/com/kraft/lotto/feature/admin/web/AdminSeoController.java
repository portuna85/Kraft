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
        return com.kraft.lotto.web.SeoPages.ALL.stream()
                .map(p -> new SitemapEntry(baseUrl + p.path(), p.changefreq(), p.priority()))
                .toList();
    }

    public record SitemapEntry(String url, String changefreq, String priority) {}
}
