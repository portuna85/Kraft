"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import { getDeviceToken } from "@/lib/device-token";
import { browserFetch, BrowserApiError } from "@/lib/browser-api";

type SavedNumber = {
  id: number;
  numbers: number[];
  label: string | null;
  source: string;
  createdAt: string;
};

type SavedNumberMatchResult = {
  savedNumber: SavedNumber;
  round: number;
  drawDate: string;
  drawNumbers: number[];
  bonusNumber: number;
  matchedCount: number;
  bonusMatch: boolean;
  prizeTier: string;
};

type MatchState = "idle" | "success" | "error";

function isWin(prizeTier: string): boolean {
  return prizeTier !== "낙첨";
}

const RECENT_ROUND_OPTIONS = 20;

type Props = {
  latestRound: number;
};

export function SavedNumbersClient({ latestRound }: Props) {
  const [items, setItems] = useState<SavedNumber[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [selectedRound, setSelectedRound] = useState<string>("latest");
  const [customRoundInput, setCustomRoundInput] = useState("");
  const [matchMap, setMatchMap] = useState<Map<number, SavedNumberMatchResult>>(new Map());
  const [matchState, setMatchState] = useState<MatchState>("idle");
  const matchFetchSeqRef = useRef(0);

  useEffect(() => {
    browserFetch<SavedNumber[]>("/api/v1/saved", {
      headers: { "X-Device-Token": getDeviceToken() },
    })
      .then((savedItems) => {
        setItems(savedItems);
        setHasError(false);
      })
      .catch(() => {
        setHasError(true);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  const fetchMatches = useCallback(() => {
    if (items.length === 0) {
      return;
    }

    const seq = ++matchFetchSeqRef.current;

    browserFetch<SavedNumberMatchResult[]>(
      `/api/v1/saved/matches?round=${encodeURIComponent(selectedRound)}`,
      { headers: { "X-Device-Token": getDeviceToken() } },
    )
      .then((results) => {
        if (seq !== matchFetchSeqRef.current) return;
        const map = new Map<number, SavedNumberMatchResult>();
        for (const result of results) {
          map.set(result.savedNumber.id, result);
        }
        setMatchMap(map);
        setMatchState("success");
      })
      .catch(() => {
        if (seq !== matchFetchSeqRef.current) return;
        // 실패해도 이전에 성공한 matchMap은 그대로 유지 — "불러오기 실패"가
        // "대조 결과 없음"처럼 보이지 않게 한다.
        setMatchState("error");
      });
  }, [items, selectedRound]);

  useEffect(() => {
    fetchMatches();
  }, [fetchMatches]);

  async function handleDelete(item: SavedNumber) {
    setItems((prev) => prev.filter((x) => x.id !== item.id));
    try {
      await browserFetch(`/api/v1/saved/${item.id}`, {
        method: "DELETE",
        headers: { "X-Device-Token": getDeviceToken() },
      });
    } catch (err) {
      if (err instanceof BrowserApiError || err instanceof Error) {
        setItems((prev) =>
          [...prev, item].sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
        );
      }
    }
  }

  function applyCustomRound(event: React.FormEvent) {
    event.preventDefault();
    const round = Number.parseInt(customRoundInput.trim(), 10);
    if (Number.isNaN(round) || round < 1 || round > latestRound) {
      return;
    }
    setSelectedRound(String(round));
    setCustomRoundInput("");
  }

  const recentRoundOptions: number[] = [];
  for (let round = latestRound; round >= 1 && recentRoundOptions.length < RECENT_ROUND_OPTIONS; round--) {
    recentRoundOptions.push(round);
  }

  return (
    <div className="saved-layout">
      {isLoading ? (
        <p className="saved-empty-state">저장된 번호를 불러오는 중입니다.</p>
      ) : hasError ? (
        <p className="saved-empty-state">저장 번호를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
      ) : items.length === 0 ? (
        <p className="saved-empty-state">아직 저장한 번호가 없습니다. 추천 페이지에서 조합을 저장해 보세요.</p>
      ) : (
        <>
          {latestRound > 0 ? (
            <div className="saved-round-controls">
              <label className="saved-round-selector">
                대조할 회차
                <select
                  value={selectedRound}
                  onChange={(event) => setSelectedRound(event.target.value)}
                >
                  <option value="latest">최신 회차</option>
                  {recentRoundOptions.map((round) => (
                    <option key={round} value={round}>
                      {round}회
                    </option>
                  ))}
                </select>
              </label>
              <form onSubmit={applyCustomRound} className="saved-round-custom-form">
                <input
                  type="number"
                  min={1}
                  max={latestRound}
                  value={customRoundInput}
                  onChange={(event) => setCustomRoundInput(event.target.value)}
                  placeholder="회차 직접 입력"
                  aria-label="회차 직접 입력"
                />
                <button type="submit" className="button secondary">
                  적용
                </button>
              </form>
              {matchState === "error" ? (
                <p className="saved-match-error" aria-live="polite">
                  대조 결과를 불러오지 못했습니다.{" "}
                  <button type="button" className="button secondary" onClick={fetchMatches}>
                    다시 시도
                  </button>
                </p>
              ) : null}
            </div>
          ) : null}

          <ul className="saved-list">
            {items.map((item) => {
              const match = matchMap.get(item.id);
              return (
                <li key={item.id} className="saved-item">
                  <div className="saved-item-row">
                    <LottoBalls numbers={item.numbers} />
                    <button
                      type="button"
                      className="saved-delete-btn"
                      onClick={() => handleDelete(item)}
                      aria-label="삭제"
                    >
                      삭제
                    </button>
                  </div>
                  {match ? (
                    <div className="saved-match-info">
                      <span className="saved-draw-ref">{match.round}회 ({match.drawDate})</span>
                      <span className={`saved-prize-badge${isWin(match.prizeTier) ? " prize-win" : ""}`}>
                        {match.prizeTier}
                      </span>
                      <span>{match.matchedCount}개 일치</span>
                      {match.bonusMatch ? <span>보너스 일치</span> : null}
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ul>
        </>
      )}
    </div>
  );
}
