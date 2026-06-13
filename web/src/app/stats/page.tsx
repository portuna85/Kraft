import type { Metadata } from "next";
import { getPatternStats } from "@/lib/api";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "패턴 통계",
  description: "홀짝 분포, 번호대 분포 등 로또 패턴 통계를 분석합니다.",
  alternates: { canonical: "/stats" }
};

function PatternSection({ title, buckets, totalRounds }: {
  title: string;
  buckets: { bucketKey: string; count: number }[];
  totalRounds: number;
}) {
  const sorted = [...buckets].sort((a, b) => a.bucketKey.localeCompare(b.bucketKey));
  return (
    <div className="pattern-section">
      <h2 className="section-title">{title}</h2>
      <ul className="pattern-list">
        {sorted.map((b) => (
          <li key={b.bucketKey} className="pattern-item">
            <span className="pattern-key">{b.bucketKey}</span>
            <span className="pattern-count">{b.count}회</span>
            <span className="pattern-pct">
              ({totalRounds > 0 ? ((b.count / totalRounds) * 100).toFixed(1) : "0.0"}%)
            </span>
          </li>
        ))}
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
      <p className="page-subtitle">총 {stats.totalRounds}회차 기준 패턴 분포입니다.</p>
      <PatternSection title="홀수 개수 분포" buckets={stats.oddCounts} totalRounds={stats.totalRounds} />
      <PatternSection title="고번호(23-45) 개수 분포" buckets={stats.highCounts} totalRounds={stats.totalRounds} />
      <PatternSection title="합계 구간 분포" buckets={stats.sumBuckets} totalRounds={stats.totalRounds} />
    </section>
  );
}
