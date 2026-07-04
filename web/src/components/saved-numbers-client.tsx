"use client";

import { useEffect, useState } from "react";
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

function isWin(prizeTier: string): boolean {
  return prizeTier !== "낙첨";
}

type Props = {
  latestRound: number;
};

export function SavedNumbersClient({ latestRound }: Props) {
  const [items, setItems] = useState<SavedNumber[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);
  const [selectedRound, setSelectedRound] = useState<string>("latest");
  const [matchMap, setMatchMap] = useState<Map<number, SavedNumberMatchResult>>(new Map());

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

  useEffect(() => {
    if (items.length === 0) {
      return;
    }

    browserFetch<SavedNumberMatchResult[]>(
      `/api/v1/saved/matches?round=${encodeURIComponent(selectedRound)}`,
      { headers: { "X-Device-Token": getDeviceToken() } },
    )
      .then((results) => {
        const map = new Map<number, SavedNumberMatchResult>();
        for (const result of results) {
          map.set(result.savedNumber.id, result);
        }
        setMatchMap(map);
      })
      .catch(() => {
        setMatchMap(new Map());
      });
  }, [items, selectedRound]);

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

  const roundOptions: number[] = [];
  for (let round = latestRound; round >= 1; round--) {
    roundOptions.push(round);
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
            <label className="saved-round-selector">
              대조할 회차
              <select
                value={selectedRound}
                onChange={(event) => setSelectedRound(event.target.value)}
              >
                <option value="latest">최신 회차</option>
                {roundOptions.map((round) => (
                  <option key={round} value={round}>
                    {round}회
                  </option>
                ))}
              </select>
            </label>
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
