package com.kraft.saved;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedNumberClientLockRepository extends JpaRepository<SavedNumberClientLock, String> {

    /**
     * 잠금 행이 없으면 만들고, 있으면 last_used_at만 갱신한다(upsert). INSERT ... ON DUPLICATE
     * KEY UPDATE라 중복 키 충돌이 예외 없이 조용히 처리되므로, 같은 클라이언트의 첫 저장이
     * 동시에 들어와도 이 문장 자체가 데드락을 만들지 않는다(아래 lockByClientTokenHash처럼
     * 이미 존재하는 행을 잠그는 것과 달리, 이 문장은 갭 락 위에 두 트랜잭션이 서로 기다리는
     * 상황을 만들지 않는다). last_used_at을 매 저장마다 갱신해야 고아 정리 스케줄러(P1-06)가
     * "충분히 오래 안 쓰였는지"를 정확히 판단할 수 있다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "INSERT INTO saved_number_client_locks (client_token_hash, created_at, last_used_at) "
            + "VALUES (:clientTokenHash, :now, :now) "
            + "ON DUPLICATE KEY UPDATE last_used_at = :now",
            nativeQuery = true)
    void ensureLockRowExists(@Param("clientTokenHash") String clientTokenHash, @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from SavedNumberClientLock l where l.clientTokenHash = :clientTokenHash")
    Optional<SavedNumberClientLock> lockByClientTokenHash(@Param("clientTokenHash") String clientTokenHash);

    /**
     * 저장번호를 모두 지운 뒤에도 영구히 남는 고아 잠금 행을 정리한다(P1-06) — 대상 클라이언트의
     * saved_numbers 행이 하나도 없고, 마지막 사용 이후 cutoff보다 오래된 행만 삭제한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE l FROM saved_number_client_locks l "
            + "WHERE l.last_used_at < :cutoff "
            + "AND NOT EXISTS (SELECT 1 FROM saved_numbers s WHERE s.client_token_hash = l.client_token_hash)",
            nativeQuery = true)
    int deleteOrphansOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
