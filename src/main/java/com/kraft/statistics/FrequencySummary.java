package com.kraft.statistics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "winning_number_frequency_summary")
public class FrequencySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ball_number", nullable = false, unique = true)
    private Integer ballNumber;

    @Column(name = "frequency", nullable = false)
    private Integer frequency;

    @Column(name = "last_round", nullable = false)
    private Integer lastRound;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected FrequencySummary() {
    }

    public FrequencySummary(Integer ballNumber, Integer frequency, Integer lastRound, OffsetDateTime updatedAt) {
        this.ballNumber = ballNumber;
        this.frequency = frequency;
        this.lastRound = lastRound;
        this.updatedAt = updatedAt;
    }

    public void update(Integer frequency, Integer lastRound, OffsetDateTime updatedAt) {
        this.frequency = frequency;
        this.lastRound = lastRound;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Integer getBallNumber() { return ballNumber; }
    public Integer getFrequency() { return frequency; }
    public Integer getLastRound() { return lastRound; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
