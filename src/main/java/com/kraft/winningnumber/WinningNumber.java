package com.kraft.winningnumber;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "winning_numbers")
public class WinningNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "round_no", nullable = false, unique = true)
    private Integer round;

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

    @Column(name = "n1", nullable = false)
    private Integer n1;

    @Column(name = "n2", nullable = false)
    private Integer n2;

    @Column(name = "n3", nullable = false)
    private Integer n3;

    @Column(name = "n4", nullable = false)
    private Integer n4;

    @Column(name = "n5", nullable = false)
    private Integer n5;

    @Column(name = "n6", nullable = false)
    private Integer n6;

    @Column(name = "bonus_number", nullable = false)
    private Integer bonusNumber;

    @Column(name = "first_prize_amount", nullable = false)
    private Long firstPrizeAmount;

    @Column(name = "second_prize", nullable = false)
    private Long secondPrize;

    @Column(name = "second_winners", nullable = false)
    private Integer secondWinners;

    @Column(name = "total_sales", nullable = false)
    private Long totalSales;

    @Column(name = "first_accum_amount", nullable = false)
    private Long firstAccumAmount;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    @jakarta.persistence.Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected WinningNumber() {
    }

    public WinningNumber(Integer round,
                         LocalDate drawDate,
                         Integer n1,
                         Integer n2,
                         Integer n3,
                         Integer n4,
                         Integer n5,
                         Integer n6,
                         Integer bonusNumber,
                         Long firstPrizeAmount,
                         Long secondPrize,
                         Integer secondWinners,
                         Long totalSales,
                         Long firstAccumAmount,
                         String rawJson,
                         OffsetDateTime createdAt) {
        this.round = round;
        this.drawDate = drawDate;
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;
        this.n4 = n4;
        this.n5 = n5;
        this.n6 = n6;
        this.bonusNumber = bonusNumber;
        this.firstPrizeAmount = firstPrizeAmount;
        this.secondPrize = secondPrize;
        this.secondWinners = secondWinners;
        this.totalSales = totalSales;
        this.firstAccumAmount = firstAccumAmount;
        this.rawJson = rawJson;
        this.createdAt = createdAt;
    }

    public void update(LocalDate drawDate,
                       Integer n1,
                       Integer n2,
                       Integer n3,
                       Integer n4,
                       Integer n5,
                       Integer n6,
                       Integer bonusNumber,
                       Long firstPrizeAmount,
                       Long secondPrize,
                       Integer secondWinners,
                       Long totalSales,
                       Long firstAccumAmount,
                       String rawJson) {
        this.drawDate = drawDate;
        this.n1 = n1;
        this.n2 = n2;
        this.n3 = n3;
        this.n4 = n4;
        this.n5 = n5;
        this.n6 = n6;
        this.bonusNumber = bonusNumber;
        this.firstPrizeAmount = firstPrizeAmount;
        this.secondPrize = secondPrize;
        this.secondWinners = secondWinners;
        this.totalSales = totalSales;
        this.firstAccumAmount = firstAccumAmount;
        this.rawJson = rawJson;
    }

    public Long getId() {
        return id;
    }

    public Integer getRound() {
        return round;
    }

    public LocalDate getDrawDate() {
        return drawDate;
    }

    public Integer getN1() {
        return n1;
    }

    public Integer getN2() {
        return n2;
    }

    public Integer getN3() {
        return n3;
    }

    public Integer getN4() {
        return n4;
    }

    public Integer getN5() {
        return n5;
    }

    public Integer getN6() {
        return n6;
    }

    public Integer getBonusNumber() {
        return bonusNumber;
    }

    public Long getFirstPrizeAmount() {
        return firstPrizeAmount;
    }

    public Long getSecondPrize() {
        return secondPrize;
    }

    public Integer getSecondWinners() {
        return secondWinners;
    }

    public Long getTotalSales() {
        return totalSales;
    }

    public Long getFirstAccumAmount() {
        return firstAccumAmount;
    }

    public String getRawJson() {
        return rawJson;
    }

    public Long getVersion() {
        return version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
