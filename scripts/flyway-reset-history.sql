-- Run this ONCE on an existing DB before deploying the squashed migration (V1__baseline.sql).
-- It clears Flyway's history and inserts a single baseline entry so Flyway treats the
-- current schema as already at V1 and skips re-running the baseline script.
--
-- Prerequisites: schema must already be at the V18 state (all original migrations applied).
-- Usage: mysql -u<user> -p <db> < scripts/flyway-reset-history.sql

DELETE FROM flyway_schema_history;

INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
    (1, '1', 'baseline', 'BASELINE', 'V1__baseline.sql', NULL, CURRENT_USER(), NOW(), 0, 1);
