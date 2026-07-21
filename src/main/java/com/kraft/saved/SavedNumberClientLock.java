package com.kraft.saved;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * saved_numbers 자체에 범위(gap) 잠금을 거는 대신, 클라이언트당 하나씩 존재하는 이 잠금
 * 행을 레코드 락으로 잠가 저장 한도·중복 확인과 삽입을 직렬화한다(B2). 실제 데이터는
 * 담지 않고 client_token_hash 존재 자체가 잠금 대상이다.
 */
@Entity
@Table(name = "saved_number_client_locks")
public class SavedNumberClientLock {

    @Id
    @Column(name = "client_token_hash", nullable = false, length = 64)
    private String clientTokenHash;

    protected SavedNumberClientLock() {
    }

    public SavedNumberClientLock(String clientTokenHash) {
        this.clientTokenHash = clientTokenHash;
    }

    public String getClientTokenHash() {
        return clientTokenHash;
    }
}
