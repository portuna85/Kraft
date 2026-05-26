package com.kraft.lotto.feature.statistics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "winning_number_frequency_summary")
public class WinningNumberFrequencySummaryEntity {
    private static final LocalDateTime DEFAULT_UPDATED_AT = LocalDateTime.of(1970, 1, 1, 0, 0);

    @Id
    @Column(name = "ball", nullable = false)
    private Integer ball;

    @Column(name = "hit_count", nullable = false)
    private long hitCount;

    @Column(name = "last_calculated_round", nullable = false)
    private int lastCalculatedRound;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected WinningNumberFrequencySummaryEntity() {
    }

    public WinningNumberFrequencySummaryEntity(Integer ball, long hitCount, int lastCalculatedRound) {
        this(ball, hitCount, lastCalculatedRound, DEFAULT_UPDATED_AT);
    }

    public WinningNumberFrequencySummaryEntity(Integer ball,
                                               long hitCount,
                                               int lastCalculatedRound,
                                               LocalDateTime updatedAt) {
        this.ball = ball;
        this.hitCount = hitCount;
        this.lastCalculatedRound = lastCalculatedRound;
        this.updatedAt = updatedAt;
    }

    public Integer getBall() {
        return ball;
    }

    public long getHitCount() {
        return hitCount;
    }

    public int getLastCalculatedRound() {
        return lastCalculatedRound;
    }
}
