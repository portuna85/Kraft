import type { Metadata } from "next";
import { getCompanionStats } from "@/lib/api";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "동반 출현 통계 | KRAFT Lotto",
  description: "두 번호가 함께 당첨되는 빈도를 분석합니다.",
  alternates: { canonical: "/companion" }
};

function ballColorClass(n: number): string {
  if (n <= 10) return "";
  if (n <= 20) return "ball-blue";
  if (n <= 30) return "ball-red";
  if (n <= 40) return "ball-gray";
  return "ball-green";
}

export default async function CompanionPage() {
  const stats = await getCompanionStats();
  const pairs = stats.topPairs.slice(0, 50);

  return (
    <section className="panel">
      <p className="eyebrow">통계</p>
      <h1 className="page-title">동반 출현</h1>
      <p className="page-subtitle">
        총 {stats.totalRounds}회차 기준 — 가장 자주 함께 당첨된 번호 쌍 상위 {pairs.length}개입니다.
      </p>
      <ol className="companion-list">
        {pairs.map((pair, idx) => {
          const pct = stats.totalRounds > 0
            ? ((pair.coCount / stats.totalRounds) * 100).toFixed(1)
            : "0.0";
          return (
            <li key={`${pair.ballA}-${pair.ballB}`} className="companion-item">
              <span className="rank">{idx + 1}</span>
              <div className="pair-balls">
                <span className={`ball ball-sm ${ballColorClass(pair.ballA)}`}>
                  {pair.ballA}
                </span>
                <span className="pair-sep">×</span>
                <span className={`ball ball-sm ${ballColorClass(pair.ballB)}`}>
                  {pair.ballB}
                </span>
              </div>
              <div className="pair-info">
                <span className="pair-count">{pair.coCount}회 동반</span>
                <span className="pair-pct">{pct}%</span>
              </div>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
