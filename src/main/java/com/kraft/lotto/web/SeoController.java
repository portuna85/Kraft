package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoController {

    private static final String DEFAULT_BASE_URL = "https://www.kraft.io.kr";
    private static final DateTimeFormatter LASTMOD_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final Environment environment;
    private final WinningNumberQueryService winningNumberQueryService;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String nl = System.lineSeparator();
        String baseUrl = publicBaseUrl();
        String lastmod = latestDrawDateIso();
        String body = String.join(nl,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">",
                sitemapUrl(baseUrl, "/",          "daily",  "1.0", lastmod),
                sitemapUrl(baseUrl, "/frequency", "weekly", "0.8", lastmod),
                sitemapUrl(baseUrl, "/rounds",    "daily",  "0.8", lastmod),
                sitemapUrl(baseUrl, "/stats",     "weekly", "0.7", lastmod),
                sitemapUrl(baseUrl, "/analysis",  "weekly", "0.7", lastmod),
                sitemapUrl(baseUrl, "/companion", "weekly", "0.6", lastmod),
                sitemapUrl(baseUrl, "/news",      "daily",  "0.7", lastmod),
                "</urlset>"
        ) + nl;
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String nl = System.lineSeparator();
        String body = String.join(nl,
                "User-agent: *",
                "Allow: /",
                "Disallow: /admin",
                "Disallow: /ops",
                "Disallow: /actuator",
                "Disallow: /fragments/",
                "",
                "Sitemap: " + publicBaseUrl() + "/sitemap.xml"
        ) + nl;
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }

    private String latestDrawDateIso() {
        return winningNumberQueryService.findLatest()
                .map(dto -> dto.drawDate().format(LASTMOD_FORMAT))
                .orElse(null);
    }

    private static String sitemapUrl(String baseUrl, String path, String changefreq, String priority,
                                     String lastmod) {
        StringBuilder sb = new StringBuilder();
        sb.append("    <url>").append(System.lineSeparator());
        sb.append("        <loc>").append(xmlEscape(baseUrl + path)).append("</loc>").append(System.lineSeparator());
        if (lastmod != null) {
            sb.append("        <lastmod>").append(lastmod).append("</lastmod>").append(System.lineSeparator());
        }
        sb.append("        <changefreq>").append(changefreq).append("</changefreq>").append(System.lineSeparator());
        sb.append("        <priority>").append(priority).append("</priority>").append(System.lineSeparator());
        sb.append("    </url>");
        return sb.toString();
    }

    private String publicBaseUrl() {
        String configured = environment.getProperty("kraft.public-base-url", DEFAULT_BASE_URL);
        String trimmed = trimTrailingSlash(configured);
        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if ((scheme == null) || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return DEFAULT_BASE_URL;
            }
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getHost().isBlank()) {
                return DEFAULT_BASE_URL;
            }
            if (uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null) {
                return DEFAULT_BASE_URL;
            }
            return trimmed;
        } catch (IllegalArgumentException ignored) {
            return DEFAULT_BASE_URL;
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String xmlEscape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
