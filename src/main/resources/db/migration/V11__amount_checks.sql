-- 음수 금액/회차 방지. ALTER TABLE ADD CONSTRAINT는 MariaDB가 기존 행 전체를 검증하므로
-- 위반 데이터가 있으면 이 마이그레이션 자체가 실패한다(배포 파이프라인의 자동 롤백이 처리).
ALTER TABLE winning_numbers
    ADD CONSTRAINT chk_wn_amounts CHECK (
        first_prize_amount >= 0 AND second_prize >= 0 AND second_winners >= 0
        AND total_sales >= 0 AND first_accum_amount >= 0 AND round_no > 0);
