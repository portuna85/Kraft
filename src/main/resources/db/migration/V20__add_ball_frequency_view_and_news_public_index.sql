-- Ball frequency view: UNION ALL fallback query를 단일 뷰로 캡슐화
-- 직접 native query 대신 이 뷰를 참조하면 칼럼별 구문 중복을 제거할 수 있다
CREATE OR REPLACE VIEW v_ball_frequencies AS
SELECT sub.ball                 AS ball,
       SUM(sub.hit)             AS hit_count
FROM (
    SELECT n1 AS ball, COUNT(*) AS hit FROM winning_numbers GROUP BY n1
    UNION ALL SELECT n2, COUNT(*) FROM winning_numbers GROUP BY n2
    UNION ALL SELECT n3, COUNT(*) FROM winning_numbers GROUP BY n3
    UNION ALL SELECT n4, COUNT(*) FROM winning_numbers GROUP BY n4
    UNION ALL SELECT n5, COUNT(*) FROM winning_numbers GROUP BY n5
    UNION ALL SELECT n6, COUNT(*) FROM winning_numbers GROUP BY n6
) sub
GROUP BY sub.ball;

-- 뉴스 공개 목록 쿼리용 복합 인덱스
-- approved/rejected 조합 필터링 후 날짜 정렬 패턴에 최적화
CREATE INDEX idx_news_public_list
    ON news_articles (approved, rejected, pub_date, collected_at);
