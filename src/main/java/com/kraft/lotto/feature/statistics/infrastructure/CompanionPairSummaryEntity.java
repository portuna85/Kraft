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
@Table(name = "companion_pair_summary")
public class CompanionPairSummaryEntity {

    @Embeddable
    public static class Id implements Serializable {

        @Column(name = "ball", nullable = false)
        private int ball;

        @Column(name = "other_ball", nullable = false)
        private int otherBall;

        protected Id() {}

        public Id(int ball, int otherBall) {
            this.ball = ball;
            this.otherBall = otherBall;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Id id)) {
                return false;
            }
            return ball == id.ball && otherBall == id.otherBall;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ball, otherBall);
        }
    }

    @EmbeddedId
    private Id id;

    @Column(name = "hit_count", nullable = false)
    private long hitCount;

    @Column(name = "last_calculated_round", nullable = false)
    private int lastCalculatedRound;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CompanionPairSummaryEntity() {}

    public CompanionPairSummaryEntity(int ball, int otherBall,
                                      long hitCount, int lastCalculatedRound,
                                      LocalDateTime updatedAt) {
        this.id = new Id(ball, otherBall);
        this.hitCount = hitCount;
        this.lastCalculatedRound = lastCalculatedRound;
        this.updatedAt = updatedAt;
    }

    public int getBall() { return id.ball; }
    public int getOtherBall() { return id.otherBall; }
    public long getHitCount() { return hitCount; }
    public int getLastCalculatedRound() { return lastCalculatedRound; }
}
