package com.kraft.lotto.feature.news.infrastructure;

import com.kraft.lotto.feature.news.domain.NewsSourceTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_articles")
public class NewsArticleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "link", nullable = false, length = 2000)
    private String link;

    @Column(name = "link_hash", nullable = false, length = 64, unique = true, columnDefinition = "VARCHAR(64)")
    private String linkHash;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source", length = 200)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_tier", nullable = false, length = 20)
    private NewsSourceTier sourceTier = NewsSourceTier.GENERAL;

    @Column(name = "pub_date")
    private LocalDateTime pubDate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    protected NewsArticleEntity() {
    }

    public NewsArticleEntity(String title,
                              String link,
                              String linkHash,
                              String description,
                              String source,
                              LocalDateTime pubDate,
                              LocalDateTime collectedAt) {
        this(title, link, linkHash, description, source, NewsSourceTier.GENERAL, pubDate, collectedAt);
    }

    public NewsArticleEntity(String title,
                              String link,
                              String linkHash,
                              String description,
                              String source,
                              NewsSourceTier sourceTier,
                              LocalDateTime pubDate,
                              LocalDateTime collectedAt) {
        this.title = title;
        this.link = link;
        this.linkHash = linkHash;
        this.description = description;
        this.source = source;
        this.sourceTier = sourceTier == null ? NewsSourceTier.GENERAL : sourceTier;
        this.pubDate = pubDate;
        this.collectedAt = collectedAt;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getLink() { return link; }
    public String getLinkHash() { return linkHash; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
    public NewsSourceTier getSourceTier() { return sourceTier == null ? NewsSourceTier.GENERAL : sourceTier; }
    public LocalDateTime getPubDate() { return pubDate; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}
