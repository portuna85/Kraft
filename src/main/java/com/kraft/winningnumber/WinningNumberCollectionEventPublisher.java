package com.kraft.winningnumber;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link WinningNumbersCollectedEvent}를 짧은 단독 트랜잭션 안에서 발행한다.
 * AFTER_COMMIT 리스너(ISR 재검증, 통계 재집계)는 발행 시점에 활성 트랜잭션이 있어야
 * 동작하는데, {@link WinningNumberCollectionService}는 외부 HTTP fetch를 트랜잭션
 * 밖에서 수행하도록 class-level {@code @Transactional}을 두지 않는다. 같은 빈 안에서의
 * self-invocation은 {@code @Transactional} AOP 프록시를 우회하므로 별도 빈으로 분리했다.
 */
@Component
public class WinningNumberCollectionEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public WinningNumberCollectionEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(WinningNumbersCollectedEvent event) {
        eventPublisher.publishEvent(event);
    }
}
