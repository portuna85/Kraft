package com.kraft.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pattern_stats_summary")
public class PatternStatsSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_type", nullable = false)
    private String statType;

    @Column(name = "bucket_key", nullable = false)
    private String bucketKey;

    @Column(name = "count_val", nullable = false)
    private Integer countVal;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PatternStatsSummary() {
    }

    public PatternStatsSummary(String statType, String bucketKey, Integer countVal, OffsetDateTime updatedAt) {
        this.statType = statType;
        this.bucketKey = bucketKey;
        this.countVal = countVal;
        this.updatedAt = updatedAt;
    }

    public void update(Integer countVal, OffsetDateTime updatedAt) {
        this.countVal = countVal;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getStatType() { return statType; }
    public String getBucketKey() { return bucketKey; }
    public Integer getCountVal() { return countVal; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
