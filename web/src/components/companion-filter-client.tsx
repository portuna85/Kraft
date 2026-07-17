"use client";

import { useRef, useState } from "react";
import { ballColorClass } from "@/lib/ball-color";
import type { CompanionPair, CompanionStatsResponse } from "@/lib/api";
import { browserFetch } from "@/lib/browser-api";

type Props = {
  pairs: CompanionPair[];
  totalRounds: number;
};

type FilterState =
  | { status: "idle" }
  | { status: "loading"; ball: number }
  | { status: "success"; ball: number; pairs: CompanionPair[] }
  | { status: "error"; ball: number };

export function CompanionFilterClient({ pairs, totalRounds }: Props) {
  const [selected, setSelected] = useState<number | null>(null);
  const [filterState, setFilterState] = useState<FilterState>({ status: "idle" });
  const fetchSeqRef = useRef(0);

  function fetchPairsForBall(ball: number) {
    const seq = ++fetchSeqRef.current;
    setFilterState({ status: "loading", ball });

    browserFetch<CompanionStatsResponse>(`/api/v1/stats/companion?ball=${ball}`)
      .then((data) => {
        if (seq !== fetchSeqRef.current) return;
        setFilterState({ status: "success", ball, pairs: data.topPairs });
      })
      .catch(() => {
        if (seq !== fetchSeqRef.current) return;
        setFilterState({ status: "error", ball });
      });
  }

  function selectNumber(number: number) {
    if (selected === number) {
      fetchSeqRef.current++; // 진행 중인 요청 결과를 무시하도록 시퀀스만 올림
      setSelected(null);
      setFilterState({ status: "idle" });
      return;
    }

    setSelected(number);
    fetchPairsForBall(number);
  }

  function retry() {
    if (selected !== null) {
      fetchPairsForBall(selected);
    }
  }

  // 선택 없음: 초기에 전달받은 상위 50개만 표시(과한 목록 방지). 번호 선택: success 상태일 때만
  // 서버 필터 결과를 사용 — loading/error 동안에는 절대 "기록 없음"을 오표시하지 않는다.
  const filtered = selected === null
    ? pairs
    : filterState.status === "success"
      ? filterState.pairs
      : null;

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
        {filterState.status === "loading" && (
          <p className="muted" aria-live="polite">해당 번호의 동반 출현 데이터를 불러오는 중...</p>
        )}
        {filterState.status === "error" && (
          <p className="muted" aria-live="polite">
            데이터를 불러오지 못했습니다.{" "}
            <button type="button" className="button secondary" onClick={retry}>
              다시 시도
            </button>
          </p>
        )}
        {selected !== null && (
          <button
            type="button"
            className="button secondary companion-clear"
            onClick={() => {
              setSelected(null);
              setFilterState({ status: "idle" });
            }}
          >
            필터 해제
          </button>
        )}
      </div>

      {filtered !== null && (
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
      )}
    </>
  );
}
