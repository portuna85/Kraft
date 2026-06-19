"use client";

import { useState } from "react";
import { ballColorClass } from "@/lib/ball-color";
import type { CompanionPair } from "@/lib/api";

type Props = {
  pairs: CompanionPair[];
  totalRounds: number;
};

export function CompanionFilterClient({ pairs, totalRounds }: Props) {
  const [selected, setSelected] = useState<number | null>(null);

  const filtered = selected === null
    ? pairs
    : pairs.filter((pair) => pair.ballA === selected || pair.ballB === selected);

  return (
    <>
      <div className="companion-filter">
        <p className="companion-filter-label">번호로 필터</p>
        <div className="companion-filter-balls">
          {Array.from({ length: 45 }, (_, index) => index + 1).map((number) => (
            <button
              key={number}
              type="button"
              onClick={() => setSelected(selected === number ? null : number)}
              className={`ball ball-sm ${ballColorClass(number)}${selected === number ? " ball-selected" : ""}`}
              aria-pressed={selected === number}
            >
              {number}
            </button>
          ))}
        </div>
        {selected !== null && (
          <button type="button" className="button secondary companion-clear" onClick={() => setSelected(null)}>
            필터 해제
          </button>
        )}
      </div>

      <ol className="companion-list">
        {filtered.map((pair, index) => {
          const pct = totalRounds > 0
            ? ((pair.coCount / totalRounds) * 100).toFixed(1)
            : "0.0";

          return (
            <li key={`${pair.ballA}-${pair.ballB}`} className="companion-item">
              <span className="rank">{index + 1}</span>
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
                <span className="pair-count">{pair.coCount}회 동반 출현</span>
                <span className="pair-pct">{pct}%</span>
              </div>
            </li>
          );
        })}

        {filtered.length === 0 && (
          <li className="companion-item companion-empty">
            <p className="muted">해당 번호를 포함한 동반 출현 기록이 없습니다.</p>
          </li>
        )}
      </ol>
    </>
  );
}
