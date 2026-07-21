package com.kraft.saved;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedNumberClientLockRepository extends JpaRepository<SavedNumberClientLock, String> {

    /**
     * 잠금 행이 없으면 만든다. INSERT IGNORE라 중복 키 충돌이 예외 없이 조용히 무시되므로,
     * 같은 클라이언트의 첫 저장이 동시에 들어와도 이 문장 자체가 데드락을 만들지 않는다
     * (아래 lockByClientTokenHash처럼 이미 존재하는 행을 잠그는 것과 달리, 이 문장은 갭 락 위에
     * 두 트랜잭션이 서로 기다리는 상황을 만들지 않는다).
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO saved_number_client_locks (client_token_hash) VALUES (:clientTokenHash)",
            nativeQuery = true)
    void ensureLockRowExists(@Param("clientTokenHash") String clientTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from SavedNumberClientLock l where l.clientTokenHash = :clientTokenHash")
    Optional<SavedNumberClientLock> lockByClientTokenHash(@Param("clientTokenHash") String clientTokenHash);
}
