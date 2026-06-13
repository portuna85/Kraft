import type { Metadata } from "next";
import { getCompanionStats } from "@/lib/api";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "동반 출현 통계",
  description: "두 번호가 함께 당첨되는 빈도를 분석합니다.",
  alternates: { canonical: "/companion" }
};

export default async function CompanionPage() {
  const stats = await getCompanionStats();

  return (
    <section className="panel">
      <p className="eyebrow">통계</p>
      <h1 className="page-title">동반 출현</h1>
      <p className="page-subtitle">
        총 {stats.totalRounds}회차 기준 — 가장 자주 함께 당첨된 번호 쌍 상위 {stats.topPairs.length}개입니다.
      </p>
      <ol className="companion-list">
        {stats.topPairs.map((pair, idx) => (
          <li key={`${pair.ballA}-${pair.ballB}`} className="companion-item">
            <span className="rank">{idx + 1}</span>
            <span className="pair-balls">{pair.ballA} · {pair.ballB}</span>
            <span className="pair-count">{pair.coCount}회 동반</span>
          </li>
        ))}
      </ol>
    </section>
  );
}
