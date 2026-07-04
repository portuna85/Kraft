"use client";

import { useState } from "react";
import { ballColorClass } from "@/lib/ball-color";
import type { CompanionPair, CompanionStatsResponse } from "@/lib/api";

type Props = {
  pairs: CompanionPair[];
  totalRounds: number;
};

export function CompanionFilterClient({ pairs, totalRounds }: Props) {
  const [selected, setSelected] = useState<number | null>(null);
  const [fullPairs, setFullPairs] = useState<CompanionPair[] | null>(null);
  const [isLoadingFull, setIsLoadingFull] = useState(false);

  function selectNumber(number: number) {
    const next = selected === number ? null : number;
    setSelected(next);

    if (next !== null && fullPairs === null && !isLoadingFull) {
      setIsLoadingFull(true);
      fetch("/api/v1/stats/companion")
        .then((res) => (res.ok ? (res.json() as Promise<CompanionStatsResponse>) : null))
        .then((data) => {
          if (data) setFullPairs(data.topPairs);
        })
        .catch(() => {
          // 지연 fetch 실패 시 초기 상위 50개 기준으로만 필터링 (기록 없음 오표시 가능)
        })
        .finally(() => setIsLoadingFull(false));
    }
  }

  // 선택 없음: 초기에 전달받은 상위 50개만 표시(과한 목록 방지). 번호 선택: 지연 fetch한
  // 전체 990쌍에서 검색해 상위 50개 밖의 번호도 정확히 매칭한다(로딩 중에는 초기 목록으로 폴백).
  const filtered = selected === null
    ? pairs
    : (fullPairs ?? pairs).filter((pair) => pair.ballA === selected || pair.ballB === selected);

  return (
    <>
      <div className="companion-filter">
        <p className="companion-filter-label">번호로 필터</p>
        <div className="companion-filter-balls">
          {Array.from({ length: 45 }, (_, index) => index + 1).map((number) => (
            <button
              key={number}
              type="button"
              onClick={() => selectNumber(number)}
              className={`ball ball-sm ${ballColorClass(number)}${selected === number ? " ball-selected" : ""}`}
              aria-pressed={selected === number}
            >
              {number}
            </button>
          ))}
        </div>
        {selected !== null && isLoadingFull && (
          <p className="muted">전체 조합을 불러오는 중...</p>
        )}
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
