"use client";

import { useEffect, useState } from "react";
import { useTransition } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import type { FrequencyStatsResponse, BallFrequency } from "@/lib/api";
import { ballColorClass } from "@/lib/ball-color";

const FILTERS = [
  { label: "전체", value: null },
  { label: "최근 100회", value: 100 },
  { label: "최근 200회", value: 200 },
  { label: "최근 500회", value: 500 },
] as const;

type Props = {
  initial: FrequencyStatsResponse;
};

function BallWithStats({ item, sampleSize }: { item: BallFrequency; sampleSize: number }) {
  const pct = sampleSize > 0 ? ((item.frequency / sampleSize) * 100).toFixed(1) : "0.0";
  return (
    <div className="freq-ball-item">
      <span className={`ball ball-sm ${ballColorClass(item.ballNumber)}`}>{item.ballNumber}</span>
      <span className="freq-count">{item.frequency}회</span>
      <span className="freq-pct">{pct}%</span>
    </div>
  );
}

async function checkCombination(numbers: number[]): Promise<boolean | null> {
  try {
    const query = numbers.map((n) => `numbers=${n}`).join("&");
    const res = await fetch(`/api/v1/numbers/check?${query}`);
    if (!res.ok) return null;
    const payload = await res.json() as { wonFirstPrize?: boolean };
    return payload.wonFirstPrize ?? null;
  } catch {
    return null;
  }
}

function CombinationGroup({ label, items }: { label: string; items: BallFrequency[] }) {
  const numbers = items.map((item) => item.ballNumber);
  const key = numbers.join(",");
  const [won, setWon] = useState<boolean | null>(null);

  useEffect(() => {
    let cancelled = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setWon(null);
    void checkCombination(numbers).then((result) => {
      if (!cancelled) setWon(result);
    });
    return () => {
      cancelled = true;
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return (
    <div className="freq-rank-group">
      <p className="freq-rank-label">{label}</p>
      <LottoBalls numbers={numbers} />
      <p className="freq-win-record">
        {won === null ? "확인 중…" : won ? "1등 당첨 내역 있음" : "1등 당첨 내역 없음"}
      </p>
    </div>
  );
}

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
  const sampleSize = activeLimit ?? stats.totalRounds;

  const top6 = byFrequency.slice(0, 6).sort((a, b) => a.ballNumber - b.ballNumber);
  const bottom6 = [...byFrequency].reverse().slice(0, 6).sort((a, b) => a.ballNumber - b.ballNumber);

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
        <CombinationGroup label="가장 자주 나온 번호 TOP 6" items={top6} />
        <CombinationGroup label="가장 적게 나온 번호 BOTTOM 6" items={bottom6} />
      </div>

      <div className="frequency-grid">
        {byNumber.map((item) => (
          <BallWithStats key={item.ballNumber} item={item} sampleSize={sampleSize} />
        ))}
      </div>
    </>
  );
}
