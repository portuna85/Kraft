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

    @Column(length = 50)
    private String sido;

    @Column(length = 80)
    private String sigungu;

    @Column(name = "purchase_method", length = 20)
    private String purchaseMethod;

    protected WinningStoreEntity() {}

    public WinningStoreEntity(int round, int grade, String name, String address,
                               int winCount, LocalDateTime collectedAt,
                               String sido, String sigungu, String purchaseMethod) {
        this.round           = round;
        this.grade           = grade;
        this.name            = name;
        this.address         = address;
        this.winCount        = winCount;
        this.collectedAt     = collectedAt;
        this.sido            = sido;
        this.sigungu         = sigungu;
        this.purchaseMethod  = purchaseMethod;
    }

    public WinningStoreEntity(int round, int grade, String name, String address,
                               int winCount, LocalDateTime collectedAt) {
        this(round, grade, name, address, winCount, collectedAt, null, null, null);
    }

    public WinningStore toDomain() {
        return new WinningStore(round, grade, name, address, winCount, sido, sigungu, purchaseMethod);
    }

    public Integer       getRound()          { return round; }
    public Integer       getGrade()          { return grade; }
    public String        getName()           { return name; }
    public String        getAddress()        { return address; }
    public Integer       getWinCount()       { return winCount; }
    public LocalDateTime getCollectedAt()    { return collectedAt; }
    public String        getSido()           { return sido; }
    public String        getSigungu()        { return sigungu; }
    public String        getPurchaseMethod() { return purchaseMethod; }
}
