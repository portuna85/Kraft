import type { Metadata } from "next";
import { getFrequencyStats } from "@/lib/api";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "번호 빈도 통계 | KRAFT Lotto",
  description: "로또 번호별 출현 빈도를 분석합니다.",
  alternates: { canonical: "/frequency" }
};

function ballColorClass(n: number): string {
  if (n <= 10) return "";
  if (n <= 20) return "ball-blue";
  if (n <= 30) return "ball-red";
  if (n <= 40) return "ball-gray";
  return "ball-green";
}

export default async function FrequencyPage() {
  const stats = await getFrequencyStats();
  const byNumber = [...stats.frequencies].sort((a, b) => a.ballNumber - b.ballNumber);
  const byFrequency = [...stats.frequencies].sort((a, b) => b.frequency - a.frequency);
  const maxFreq = byFrequency[0]?.frequency ?? 1;

  return (
    <section className="panel">
      <p className="eyebrow">통계</p>
      <h1 className="page-title">번호 빈도</h1>
      <p className="page-subtitle">
        총 {stats.totalRounds}회차 기준 — 각 번호가 당첨번호에 포함된 누적 횟수입니다.
      </p>

      <div className="freq-summary">
        <div className="freq-rank-group">
          <p className="freq-rank-label">자주 출현 TOP 5</p>
          <div className="balls">
            {byFrequency.slice(0, 5).map((item) => (
              <span key={item.ballNumber} className={`ball ${ballColorClass(item.ballNumber)}`}>
                {item.ballNumber}
              </span>
            ))}
          </div>
        </div>
        <div className="freq-rank-group">
          <p className="freq-rank-label">드물게 출현 BOTTOM 5</p>
          <div className="balls">
            {[...byFrequency].reverse().slice(0, 5).map((item) => (
              <span key={item.ballNumber} className={`ball ${ballColorClass(item.ballNumber)}`}>
                {item.ballNumber}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="frequency-grid">
        {byNumber.map((item) => {
          const pct = ((item.frequency / stats.totalRounds) * 100).toFixed(1);
          const barWidth = Math.round((item.frequency / maxFreq) * 100);
          return (
            <div key={item.ballNumber} className="frequency-item">
              <span className={`ball ball-sm ${ballColorClass(item.ballNumber)}`}>
                {item.ballNumber}
              </span>
              <div className="bar-track">
                <div className="bar-fill" style={{ width: `${barWidth}%` }} />
              </div>
              <span className="freq-count">{item.frequency}회</span>
              <span className="freq-pct">{pct}%</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
