"use client";

import { useState } from "react";
import type { CompanionPair } from "@/lib/api";

function ballColorClass(n: number): string {
  if (n <= 10) return "";
  if (n <= 20) return "ball-blue";
  if (n <= 30) return "ball-red";
  if (n <= 40) return "ball-gray";
  return "ball-green";
}

type Props = {
  pairs: CompanionPair[];
  totalRounds: number;
};

export function CompanionFilterClient({ pairs, totalRounds }: Props) {
  const [selected, setSelected] = useState<number | null>(null);

  const filtered = selected === null
    ? pairs
    : pairs.filter((p) => p.ballA === selected || p.ballB === selected);

  return (
    <>
      <div className="companion-filter">
        <p className="companion-filter-label">번호로 필터</p>
        <div className="companion-filter-balls">
          {Array.from({ length: 45 }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              type="button"
              onClick={() => setSelected(selected === n ? null : n)}
              className={`ball ball-sm ${ballColorClass(n)}${selected === n ? " ball-selected" : ""}`}
              aria-pressed={selected === n}
            >
              {n}
            </button>
          ))}
        </div>
        {selected !== null && (
          <button type="button" className="button secondary" style={{ marginTop: "8px" }} onClick={() => setSelected(null)}>
            필터 해제
          </button>
        )}
      </div>

      <ol className="companion-list">
        {filtered.map((pair, idx) => {
          const pct = totalRounds > 0
            ? ((pair.coCount / totalRounds) * 100).toFixed(1)
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
                <span className="pair-count">{pair.coCount}회 함께 출현</span>
                <span className="pair-pct">{pct}%</span>
              </div>
            </li>
          );
        })}
        {filtered.length === 0 && (
          <li className="companion-item" style={{ justifyContent: "center" }}>
            <p className="muted">해당 번호를 포함한 동반 쌍이 없습니다.</p>
          </li>
        )}
      </ol>
    </>
  );
}
