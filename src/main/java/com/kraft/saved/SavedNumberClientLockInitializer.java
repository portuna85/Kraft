package com.kraft.saved;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 잠금 행 생성을 REQUIRES_NEW로 분리해 즉시 커밋되게 한다. SavedNumbersService의 트랜잭션
 * 안에서 그대로 실행하면, 이 INSERT IGNORE 자체가 짧게라도 걸어야 하는 잠금이 뒤이어 같은
 * 트랜잭션에서 시도하는 PESSIMISTIC_WRITE 조회와 겹쳐 불필요하게 트랜잭션을 길게 끈다.
 * 같은 빈 안에서 self-invocation으로 호출하면 {@code @Transactional} 프록시를 우회하므로
 * 별도 빈으로 둔다.
 */
@Component
public class SavedNumberClientLockInitializer {

    private final SavedNumberClientLockRepository repository;

    public SavedNumberClientLockInitializer(SavedNumberClientLockRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(String clientTokenHash) {
        repository.ensureLockRowExists(clientTokenHash);
    }
}
