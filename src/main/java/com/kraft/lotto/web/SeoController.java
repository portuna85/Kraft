package com.kraft.lotto.web;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoController {

    private static final String DEFAULT_BASE_URL = "https://kraft.io.kr";

    private final Environment environment;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String lineSeparator = System.lineSeparator();
        String baseUrl = publicBaseUrl();
        String body = String.join(
                lineSeparator,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">",
                "    <url>",
                "        <loc>" + xmlEscape(baseUrl + "/") + "</loc>",
                "    </url>",
                "</urlset>"
        ) + lineSeparator;
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        String lineSeparator = System.lineSeparator();
        String body = String.join(
                lineSeparator,
                "User-agent: *",
                "Allow: /",
                "Disallow: /admin",
                "Disallow: /ops",
                "",
                "Sitemap: " + publicBaseUrl() + "/sitemap.xml"
        ) + lineSeparator;
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
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
