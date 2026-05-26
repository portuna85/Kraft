-- ShedLock 테이블: 다중 인스턴스 분산 스케줄링을 위해 스키마만 선행 생성.
-- 현재 단일 인스턴스 운영이므로 애플리케이션이 이 테이블을 사용하지 않는다.
-- 향후 net.javacrumbs.shedlock 통합 시 이 테이블을 활용한다.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3)  NOT NULL,
    locked_at  TIMESTAMP(3)  NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);
