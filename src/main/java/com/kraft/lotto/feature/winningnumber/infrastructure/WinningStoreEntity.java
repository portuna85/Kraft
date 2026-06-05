package com.kraft.lotto.feature.winningnumber.infrastructure;

import com.kraft.lotto.feature.winningnumber.domain.WinningStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "winning_stores")
public class WinningStoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer round;

    @Column(nullable = false)
    private Integer grade;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String address;

    @Column(nullable = false)
    private Integer winCount;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    protected WinningStoreEntity() {}

    public WinningStoreEntity(int round, int grade, String name, String address, int winCount, LocalDateTime collectedAt) {
        this.round       = round;
        this.grade       = grade;
        this.name        = name;
        this.address     = address;
        this.winCount    = winCount;
        this.collectedAt = collectedAt;
    }

    public WinningStore toDomain() {
        return new WinningStore(round, grade, name, address, winCount);
    }

    public Integer       getRound()       { return round; }
    public Integer       getGrade()       { return grade; }
    public String        getName()        { return name; }
    public String        getAddress()     { return address; }
    public Integer       getWinCount()    { return winCount; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}
