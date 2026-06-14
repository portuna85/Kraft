import type { Metadata } from "next";
import { getPatternStats, type PatternBucket } from "@/lib/api";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "패턴 통계 | KRAFT Lotto",
  description: "홀짝 비율, 고저 분포, 합계 구간 등 회차 패턴을 통계로 확인합니다.",
  alternates: { canonical: "/stats" }
};

function PatternSection({
  title,
  buckets,
  totalRounds,
}: {
  title: string;
  buckets: PatternBucket[];
  totalRounds: number;
}) {
  const sorted = [...buckets].sort((a, b) => a.bucketKey.localeCompare(b.bucketKey));
  const maxCount = Math.max(...sorted.map((b) => b.count), 1);

  return (
    <div className="pattern-section">
      <h2 className="section-title">{title}</h2>
      <ul className="pattern-list">
        {sorted.map((b) => {
          const pct = totalRounds > 0
            ? ((b.count / totalRounds) * 100).toFixed(1)
            : "0.0";
          const barWidth = Math.round((b.count / maxCount) * 100);
          return (
            <li key={b.bucketKey} className="pattern-item">
              <span className="pattern-key">{b.bucketKey}</span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${barWidth}%` }} />
              </div>
              <span className="pattern-count">{b.count}회</span>
              <span className="pattern-pct">{pct}%</span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default async function StatsPage() {
  const stats = await getPatternStats();

  return (
    <section className="panel">
      <p className="eyebrow">통계</p>
      <h1 className="page-title">패턴 통계</h1>
      <p className="page-subtitle">총 {stats.totalRounds}회 기준으로 당첨 번호 조합의 분포를 정리했습니다.</p>
      <PatternSection
        title="홀수 개수 분포"
        buckets={stats.oddCounts}
        totalRounds={stats.totalRounds}
      />
      <PatternSection
        title="고번호(23-45) 포함 개수"
        buckets={stats.highCounts}
        totalRounds={stats.totalRounds}
      />
      <PatternSection
        title="번호 합계 구간 분포"
        buckets={stats.sumBuckets}
        totalRounds={stats.totalRounds}
      />
    </section>
  );
}
