package com.kraft.operationlog;

import java.time.OffsetDateTime;

/**
 * 공개 상태 페이지용 응답. 내부 운영 로그(IP·요청 ID·원문 예외 메시지 등)는
 * 절대 노출하지 않고 회차·유형·해결 여부만 보여준다.
 */
public record PublicIncidentResponse(
        Integer round,
        String type,
        boolean resolved,
        OffsetDateTime occurredAt
) {
}
