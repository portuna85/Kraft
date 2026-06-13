package com.kraft.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "companion_pair_summary")
public class CompanionPairSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ball_a", nullable = false)
    private Integer ballA;

    @Column(name = "ball_b", nullable = false)
    private Integer ballB;

    @Column(name = "co_count", nullable = false)
    private Integer coCount;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CompanionPairSummary() {
    }

    public CompanionPairSummary(Integer ballA, Integer ballB, Integer coCount, OffsetDateTime updatedAt) {
        this.ballA = ballA;
        this.ballB = ballB;
        this.coCount = coCount;
        this.updatedAt = updatedAt;
    }

    public void update(Integer coCount, OffsetDateTime updatedAt) {
        this.coCount = coCount;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Integer getBallA() { return ballA; }
    public Integer getBallB() { return ballB; }
    public Integer getCoCount() { return coCount; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
