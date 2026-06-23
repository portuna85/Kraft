package com.kraft.winningnumber;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신규 회차 INSERT만 별도의 REQUIRES_NEW 트랜잭션으로 분리 실행한다. 같은 빈
 * 안에서 self-invocation으로 호출하면 {@code @Transactional} AOP 프록시를
 * 우회하므로 별도 빈으로 둔다. {@link WinningNumberCommandService#upsertWithResult}가
 * unique 제약 위반(DataIntegrityViolationException)을 잡아 update로 재해석할 때,
 * 위반이 일어난 트랜잭션만 롤백시키고 호출자의 트랜잭션은 깨끗하게 유지하기 위함이다.
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
}
