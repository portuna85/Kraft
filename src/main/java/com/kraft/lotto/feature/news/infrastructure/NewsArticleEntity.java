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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    @Column(name = "title_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String titleHash;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "source", length = 200)
    private String source;

    @Column(name = "source_domain", length = 253)
    private String sourceDomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_tier", nullable = false, length = 20)
    private NewsSourceTier sourceTier = NewsSourceTier.GENERAL;

    @Column(name = "pub_date")
    private LocalDateTime pubDate;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "rejected", nullable = false)
    private boolean rejected;

    @Column(name = "reject_reason", length = 200)
    private String rejectReason;

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
        this(title, link, linkHash, description, source, null, sourceTier, pubDate, collectedAt);
    }

    public NewsArticleEntity(String title,
                              String link,
                              String linkHash,
                              String description,
                              String source,
                              String sourceDomain,
                              NewsSourceTier sourceTier,
                              LocalDateTime pubDate,
                              LocalDateTime collectedAt) {
        this.title = title;
        this.link = link;
        this.linkHash = linkHash;
        this.titleHash = sha256(title != null ? title : "");
        this.description = description;
        this.source = source;
        this.sourceDomain = sourceDomain;
        this.sourceTier = sourceTier == null ? NewsSourceTier.GENERAL : sourceTier;
        this.pubDate = pubDate;
        this.collectedAt = collectedAt;
        this.approved = (this.sourceTier != NewsSourceTier.GENERAL);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getLink() { return link; }
    public String getLinkHash() { return linkHash; }
    public String getTitleHash() { return titleHash; }
    public String getDescription() { return description; }
    public String getSource() { return source; }
    public String getSourceDomain() { return sourceDomain; }
    public NewsSourceTier getSourceTier() { return sourceTier == null ? NewsSourceTier.GENERAL : sourceTier; }
    public LocalDateTime getPubDate() { return pubDate; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public boolean isRejected() { return rejected; }
    public void setRejected(boolean rejected) { this.rejected = rejected; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
