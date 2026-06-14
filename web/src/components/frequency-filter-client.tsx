"use client";

import { useState, useTransition } from "react";
import type { FrequencyStatsResponse } from "@/lib/api";

const FILTERS = [
  { label: "전체", value: null },
  { label: "최근 100회", value: 100 },
  { label: "최근 200회", value: 200 },
  { label: "최근 500회", value: 500 },
] as const;

function ballColorClass(n: number): string {
  if (n <= 10) return "";
  if (n <= 20) return "ball-blue";
  if (n <= 30) return "ball-red";
  if (n <= 40) return "ball-gray";
  return "ball-green";
}

type Props = {
  initial: FrequencyStatsResponse;
};

export function FrequencyFilterClient({ initial }: Props) {
  const [stats, setStats] = useState(initial);
  const [activeLimit, setActiveLimit] = useState<number | null>(null);
  const [isPending, startTransition] = useTransition();

  function applyFilter(limit: number | null) {
    if (limit === activeLimit) return;
    startTransition(async () => {
      if (limit === null) {
        setStats(initial);
        setActiveLimit(null);
        return;
      }
      try {
        const res = await fetch(`/api/v1/stats/frequency?limit=${limit}`);
        if (!res.ok) return;
        setStats(await res.json() as FrequencyStatsResponse);
        setActiveLimit(limit);
      } catch {
        // 네트워크 오류 시 현재 상태 유지
      }
    });
  }

  const byNumber = [...stats.frequencies].sort((a, b) => a.ballNumber - b.ballNumber);
  const byFrequency = [...stats.frequencies].sort((a, b) => b.frequency - a.frequency);
  const maxFreq = byFrequency[0]?.frequency ?? 1;
  const sampleSize = activeLimit ?? stats.totalRounds;

  return (
    <>
      <div className="freq-filter-tabs" role="tablist" aria-label="조회 기간">
        {FILTERS.map(({ label, value }) => (
          <button
            key={label}
            role="tab"
            type="button"
            aria-selected={activeLimit === value}
            disabled={isPending}
            onClick={() => applyFilter(value)}
            className={`freq-filter-tab${activeLimit === value ? " active" : ""}`}
          >
            {label}
          </button>
        ))}
      </div>

      <p className="freq-filter-desc">
        {activeLimit === null
          ? `총 ${stats.totalRounds}회 전체 기준`
          : `최근 ${activeLimit}회 기준`}
        으로 각 번호가 당첨 번호에 포함된 누적 횟수입니다.
        {isPending && <span className="muted"> 불러오는 중…</span>}
      </p>

      <div className="freq-summary">
        <div className="freq-rank-group">
          <p className="freq-rank-label">가장 자주 나온 번호 TOP 5</p>
          <div className="balls">
            {byFrequency.slice(0, 5).map((item) => (
              <span key={item.ballNumber} className={`ball ${ballColorClass(item.ballNumber)}`}>
                {item.ballNumber}
              </span>
            ))}
          </div>
        </div>
        <div className="freq-rank-group">
          <p className="freq-rank-label">가장 적게 나온 번호 BOTTOM 5</p>
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
          const pct = sampleSize > 0
            ? ((item.frequency / sampleSize) * 100).toFixed(1)
            : "0.0";
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
    </>
  );
}
