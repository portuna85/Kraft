-- findByBallAOrBallBOrderByCoCountDescBallAAscBallBAsc의 "OR ball_b = ?" 절이
-- uk_companion_pair(ball_a, ball_b)를 못 타 풀스캔+파일소트가 나던 문제를 완화한다.
CREATE INDEX idx_companion_pair_ball_b ON companion_pair_summary (ball_b);
