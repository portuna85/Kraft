package com.kraft.operationlog;

/**
 * 운영 콘솔 수동 회차 저장이 성공적으로 커밋된 뒤에만 감사 로그를 남기기 위한 이벤트.
 * {@link WinningNumberOperationLogService}가 REQUIRES_NEW로 즉시 커밋하는 감사 로그를
 * outer 트랜잭션 커밋 전에 남기면, 이후 outer 트랜잭션이 실패해도 "성공" 로그가 남을 수
 * 있다(B1). 이 이벤트는 outer 트랜잭션에 참여해 발행되므로, 커밋이 실제로 성공했을 때만
 * AFTER_COMMIT 리스너가 발화한다.
 */
public record WinningNumberManualUpsertEvent(int round, String caller) {
}
