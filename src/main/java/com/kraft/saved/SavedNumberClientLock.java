package com.kraft.saved;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * saved_numbers 자체에 범위(gap) 잠금을 거는 대신, 클라이언트당 하나씩 존재하는 이 잠금
 * 행을 레코드 락으로 잠가 저장 한도·중복 확인과 삽입을 직렬화한다(B2). 실제 데이터는
 * 담지 않고 client_token_hash 존재 자체가 잠금 대상이다.
 *
 * createdAt/lastUsedAt은 고아 행(저장번호를 모두 지운 뒤에도 남는 잠금) 정리 스케줄러(P1-06)가
 * "충분히 오래 안 쓰였는지" 판단하는 기준이다.
 */
@Entity
@Table(name = "saved_number_client_locks")
public class SavedNumberClientLock {

    @Id
    @Column(name = "client_token_hash", nullable = false, length = 64)
    private String clientTokenHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private OffsetDateTime lastUsedAt;

    protected SavedNumberClientLock() {
    }

    public SavedNumberClientLock(String clientTokenHash, OffsetDateTime now) {
        this.clientTokenHash = clientTokenHash;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    public String getClientTokenHash() {
        return clientTokenHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastUsedAt() {
        return lastUsedAt;
    }
}
