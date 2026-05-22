package com.kraft.lotto.web;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeoController {

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
                "        <loc>" + baseUrl + "/</loc>",
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
                "",
                "Sitemap: " + publicBaseUrl() + "/sitemap.xml"
        ) + lineSeparator;
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }

    private String publicBaseUrl() {
        String configured = environment.getProperty("kraft.public-base-url", "https://www.kraft.io.kr");
        return trimTrailingSlash(configured);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://www.kraft.io.kr";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
