package com.kraft.winningnumber;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신규 회차 INSERT/재해석용 REQUIRES_NEW 처리를 모아둔다. 같은 빈 안에서 self-invocation으로
 * 호출하면 {@code @Transactional} AOP 프록시를 우회하므로 별도 빈으로 둔다.
 * {@link WinningNumberCommandService#upsertWithResult}가 unique 제약 위반
 * (DataIntegrityViolationException)을 잡아 update로 재해석할 때, 위반이 일어난 트랜잭션만
 * 롤백시키고 호출자의 트랜잭션은 깨끗하게 유지하기 위함이다.
 */
@Component
public class WinningNumberInsertExecutor {

    private final WinningNumberRepository winningNumberRepository;

    public WinningNumberInsertExecutor(WinningNumberRepository winningNumberRepository) {
        this.winningNumberRepository = winningNumberRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WinningNumberResponse insertNew(WinningNumber winningNumber) {
        return WinningNumberResponse.from(winningNumberRepository.save(winningNumber));
    }

    /**
     * 신규 insert가 unique 제약 위반으로 실패했을 때, 경쟁에서 이긴 트랜잭션이 커밋한 행을
     * 찾아 update로 재해석한다. 재조회와 update 저장을 모두 새 트랜잭션에서 수행해야
     * REPEATABLE READ의 오래된 조회 스냅샷 문제를 피할 수 있다(B3) — 호출자(outer) 트랜잭션은
     * 맨 처음 findByRound()로 이미 스냅샷이 고정돼 있어, 그 트랜잭션에서 재조회하면 방금
     * 커밋된 행이 안 보일 수 있다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WinningNumberUpsertResult resolveConcurrentInsert(int round,
                                                              WinningNumberUpsertRequest request,
                                                              List<Integer> normalized,
                                                              DataIntegrityViolationException fallback) {
        WinningNumber concurrent = winningNumberRepository.findByRound(round).orElseThrow(() -> fallback);
        return WinningNumberCommandService.applyUpdate(winningNumberRepository, concurrent, request, normalized);
    }
}
