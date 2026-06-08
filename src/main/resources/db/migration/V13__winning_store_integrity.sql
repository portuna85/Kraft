-- 고아 row 제거: winning_numbers에 존재하지 않는 round
DELETE FROM winning_stores
WHERE round NOT IN (
    SELECT r FROM (SELECT round AS r FROM winning_numbers) AS valid_rounds
);

-- 중복 row 정리: (round, grade, name, address, purchase_method, source) 기준 최소 id만 보존
DELETE FROM winning_stores
WHERE id NOT IN (
    SELECT min_id FROM (
        SELECT MIN(id) AS min_id
        FROM winning_stores
        GROUP BY round, grade,
                 COALESCE(name, ''),
                 COALESCE(address, ''),
                 COALESCE(purchase_method, ''),
                 COALESCE(source, '')
    ) AS dedup_min
);

-- FK: winning_stores.round → winning_numbers.round
ALTER TABLE winning_stores
    ADD CONSTRAINT fk_winning_stores_round
    FOREIGN KEY (round) REFERENCES winning_numbers(round) ON DELETE CASCADE;

-- Unique key: 동일 회차·등급·판매점 중복 방지
-- MariaDB InnoDB dynamic row format 필요 (기본값). 총 인덱스 크기 ~2488 bytes < 3072 bytes 제한.
ALTER TABLE winning_stores
    ADD UNIQUE KEY uk_winning_store_dedupe
    (round, grade, name, address, purchase_method, source);
