package com.kraft.lotto.feature.statistics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "pattern_stats_summary")
public class PatternStatsSummaryEntity {

    public static final String TYPE_ODD_EVEN = "ODD_EVEN";
    public static final String TYPE_SUM_RANGE = "SUM_RANGE";

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "stat_type", nullable = false, length = 20)
        private String statType;

        @Column(name = "bucket_key", nullable = false)
        private int bucketKey;

        protected Id() {}

        public Id(String statType, int bucketKey) {
            this.statType = statType;
            this.bucketKey = bucketKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Id id)) {
                return false;
            }
            return bucketKey == id.bucketKey && Objects.equals(statType, id.statType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statType, bucketKey);
        }
    }

    @EmbeddedId
    private Id id;

    @Column(name = "draw_count", nullable = false)
    private long drawCount;

    @Column(name = "total_draws", nullable = false)
    private long totalDraws;

    @Column(name = "last_calculated_round", nullable = false)
    private int lastCalculatedRound;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PatternStatsSummaryEntity() {}

    public PatternStatsSummaryEntity(String statType, int bucketKey,
                                     long drawCount, long totalDraws,
                                     int lastCalculatedRound, LocalDateTime updatedAt) {
        this.id = new Id(statType, bucketKey);
        this.drawCount = drawCount;
        this.totalDraws = totalDraws;
        this.lastCalculatedRound = lastCalculatedRound;
        this.updatedAt = updatedAt;
    }

    public String getStatType() { return id.statType; }
    public int getBucketKey() { return id.bucketKey; }
    public long getDrawCount() { return drawCount; }
    public long getTotalDraws() { return totalDraws; }
    public int getLastCalculatedRound() { return lastCalculatedRound; }
}
