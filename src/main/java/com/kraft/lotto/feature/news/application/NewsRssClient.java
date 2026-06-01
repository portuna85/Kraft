package com.kraft.lotto.feature.news.application;

import com.kraft.lotto.feature.news.domain.NewsArticle;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

class NewsRssClient {

    private static final Logger log = LoggerFactory.getLogger(NewsRssClient.class);

    private static final DateTimeFormatter RFC_2822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

    private final RestClient restClient;
    private final String rssUrl;
    private final int maxArticles;

    NewsRssClient(RestClient restClient, String rssUrl, int maxArticles) {
        this.restClient = restClient;
        this.rssUrl = rssUrl;
        this.maxArticles = maxArticles;
    }

    List<NewsArticle> fetch() {
        String xml = restClient.get()
                .uri(rssUrl)
                .retrieve()
                .body(String.class);
        if (xml == null || xml.isBlank()) {
            log.warn("news rss empty response url={}", rssUrl);
            return List.of();
        }
        return parse(xml);
    }

    private List<NewsArticle> parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            List<NewsArticle> result = new ArrayList<>(Math.min(items.getLength(), maxArticles));

            for (int i = 0; i < items.getLength() && result.size() < maxArticles; i++) {
                Node node = items.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element item = (Element) node;
                String title = text(item, "title");
                String link = text(item, "link");
                if (title == null || link == null || link.isBlank()) {
                    continue;
                }
                String description = stripHtml(text(item, "description"));
                String source = sourceText(item);
                LocalDateTime pubDate = parsePubDate(text(item, "pubDate"));
                result.add(new NewsArticle(null, title.trim(), link.trim(), description, source, pubDate, null));
            }
            return result;
        } catch (Exception e) {
            log.warn("news rss parse failed url={} error={}", rssUrl, e.getMessage());
            return List.of();
        }
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        Node node = nodes.item(0);
        String content = node.getTextContent();
        return content == null || content.isBlank() ? null : content.trim();
    }

    private static String sourceText(Element item) {
        NodeList nodes = item.getElementsByTagName("source");
        if (nodes.getLength() == 0) {
            return null;
        }
        String content = nodes.item(0).getTextContent();
        return (content == null || content.isBlank()) ? null : content.trim();
    }

    private static LocalDateTime parsePubDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(value, RFC_2822);
            return zdt.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        String stripped = html.replaceAll("<[^>]*>", " ").replaceAll("\\s{2,}", " ").trim();
        return stripped.isBlank() ? null : stripped;
    }
}
