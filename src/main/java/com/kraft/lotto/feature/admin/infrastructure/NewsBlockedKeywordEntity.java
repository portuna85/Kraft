package com.kraft.lotto.feature.admin.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "news_blocked_keyword")
@Getter
@NoArgsConstructor
public class NewsBlockedKeywordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", nullable = false, unique = true, length = 255)
    private String keyword;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public NewsBlockedKeywordEntity(String keyword, String reason,
                                    String createdBy, LocalDateTime createdAt) {
        this.keyword = keyword;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
