import type { Metadata } from "next";
import { getPatternStats, type PatternBucket } from "@/lib/api";
export const revalidate = 1800;

export const metadata: Metadata = {
  title: "패턴 통계 | KRAFT Lotto",
  description: "로또 6/45 당첨 번호의 홀짝 비율, 고번호 분포, 합계 구간별 빈도를 확인할 수 있습니다.",
  alternates: { canonical: "/stats" },
};

const SUM_ORDER = ["21-65", "66-110", "111-155", "156-200", "201-255"];

function PatternSection({
  title,
  buckets,
  totalRounds,
  sortOrder,
}: {
  title: string;
  buckets: PatternBucket[];
  totalRounds: number;
  sortOrder?: string[];
}) {
  const sorted = sortOrder
    ? [...buckets].sort((a, b) => sortOrder.indexOf(a.bucketKey) - sortOrder.indexOf(b.bucketKey))
    : [...buckets].sort((a, b) => Number(a.bucketKey) - Number(b.bucketKey));
  const maxCount = Math.max(...sorted.map((b) => b.count), 1);

  return (
    <div className="pattern-section">
      <h2 className="section-title">{title}</h2>
      <ul className="pattern-list">
        {sorted.map((b) => {
          const pct = totalRounds > 0 ? ((b.count / totalRounds) * 100).toFixed(1) : "0.0";
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
      <p className="eyebrow">패턴 통계</p>
      <h1 className="page-title">패턴 통계</h1>
      <p className="muted" style={{ marginTop: 0 }}>총 {stats.totalRounds}회 기준</p>
      <PatternSection
        title="홀수 개수 분포"
        buckets={stats.oddCounts}
        totalRounds={stats.totalRounds}
      />
      <PatternSection
        title="고번호 개수 분포"
        buckets={stats.highCounts}
        totalRounds={stats.totalRounds}
      />
      <PatternSection
        title="합계 구간 분포"
        buckets={stats.sumBuckets}
        totalRounds={stats.totalRounds}
        sortOrder={SUM_ORDER}
      />
    </section>
  );
}
