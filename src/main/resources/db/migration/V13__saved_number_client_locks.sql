-- B2: 저장 번호 한도·중복 확인과 삽입을 클라이언트 단위로 직렬화하기 위한 잠금 행 테이블.
-- saved_numbers 자체에 FOR UPDATE 범위 잠금을 걸면 신규 클라이언트의 첫 저장처럼 아직
-- 행이 없는 구간에 갭 락이 걸려, 동시 INSERT가 서로의 갭 락 해제를 기다리다 데드락이 날 수
-- 있다(2개 세션만으로도 재현됨). 이미 존재가 보장된 행을 잠그면(레코드 락) 이 문제가 없다.
CREATE TABLE saved_number_client_locks (
    client_token_hash CHAR(64) NOT NULL,
    PRIMARY KEY (client_token_hash)
);
