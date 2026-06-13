import type { Metadata } from "next";
import { getFrequencyStats } from "@/lib/api";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "번호 빈도 통계",
  description: "로또 번호별 출현 빈도를 분석합니다.",
  alternates: { canonical: "/frequency" }
};

export default async function FrequencyPage() {
  const stats = await getFrequencyStats();
  const sorted = [...stats.frequencies].sort((a, b) => b.frequency - a.frequency);
  const maxFreq = sorted[0]?.frequency ?? 1;

  return (
    <section className="panel">
      <p className="eyebrow">통계</p>
      <h1 className="page-title">번호 빈도</h1>
      <p className="page-subtitle">총 {stats.totalRounds}회차 기준 — 각 번호가 당첨번호에 포함된 횟수입니다.</p>
      <div className="frequency-grid">
        {sorted.map((item) => (
          <div key={item.ballNumber} className="frequency-item">
            <span className="ball">{item.ballNumber}</span>
            <div className="bar-track">
              <div
                className="bar-fill"
                style={{ width: `${Math.round((item.frequency / maxFreq) * 100)}%` }}
              />
            </div>
            <span className="freq-count">{item.frequency}회</span>
          </div>
        ))}
      </div>
    </section>
  );
}
