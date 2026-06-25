"use client";

import { useEffect, useState } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import { getDeviceToken } from "@/lib/device-token";

type SavedNumber = {
  id: number;
  numbers: number[];
  label: string | null;
  source: string;
  createdAt: string;
};

type MatchResult = {
  savedNumber: SavedNumber;
  round: number;
  drawDate: string;
  drawNumbers: number[];
  bonusNumber: number;
  matchedCount: number;
  bonusMatch: boolean;
  prizeTier: string;
};

const UNDO_WINDOW_MS = Number(process.env.NEXT_PUBLIC_UNDO_WINDOW_MS ?? 5000);

function sortByCreatedAtDesc(items: SavedNumber[]): SavedNumber[] {
  return [...items].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

function isWin(prizeTier: string) {
  return prizeTier !== "낙첨";
}

export function SavedNumbersClient() {
  const [items, setItems] = useState<SavedNumber[]>([]);
  const [matchMap, setMatchMap] = useState<Map<number, MatchResult>>(new Map());
  const [latestRound, setLatestRound] = useState<{ round: number; drawDate: string } | null>(null);
  const [message, setMessage] = useState("");
  const [pendingDelete, setPendingDelete] = useState<{ item: SavedNumber; timer: number } | null>(null);

  useEffect(() => {
    const token = getDeviceToken();
    const headers = { "X-Device-Token": token };

    const fetchSaved = fetch("/api/v1/saved", { headers })
      .then(async (res) => {
        if (!res.ok) {
          const err = (await res.json()) as { message?: string };
          throw new Error(err.message ?? "저장한 번호를 불러오지 못했습니다.");
        }
        return res.json() as Promise<SavedNumber[]>;
      });

    const fetchResults = fetch("/api/v1/saved/results", { headers })
      .then((res) => (res.ok ? (res.json() as Promise<MatchResult[]>) : Promise.resolve([])))
      .catch(() => [] as MatchResult[]);

    Promise.all([fetchSaved, fetchResults])
      .then(([saved, results]) => {
        setItems(saved);
        const map = new Map<number, MatchResult>();
        for (const r of results) {
          map.set(r.savedNumber.id, r);
        }
        setMatchMap(map);
        if (results.length > 0) {
          setLatestRound({ round: results[0].round, drawDate: results[0].drawDate });
        }
      })
      .catch((err: Error) => setMessage(err.message));
  }, []);

  async function finalizeDelete(item: SavedNumber) {
    try {
      const res = await fetch(`/api/v1/saved/${item.id}`, {
        method: "DELETE",
        headers: { "X-Device-Token": getDeviceToken() },
      });
      if (!res.ok) {
        setItems((prev) => sortByCreatedAtDesc([...prev, item]));
        setMessage("삭제에 실패했습니다.");
      }
    } catch {
      setItems((prev) => sortByCreatedAtDesc([...prev, item]));
      setMessage("삭제하지 못했습니다.");
    }
  }

  function handleDelete(item: SavedNumber) {
    if (pendingDelete) {
      window.clearTimeout(pendingDelete.timer);
      void finalizeDelete(pendingDelete.item);
    }
    setItems((prev) => prev.filter((x) => x.id !== item.id));
    const timer = window.setTimeout(() => {
      setPendingDelete(null);
      void finalizeDelete(item);
    }, UNDO_WINDOW_MS);
    setPendingDelete({ item, timer });
    setMessage("삭제했습니다.");
  }

  function handleUndoDelete() {
    if (!pendingDelete) return;
    window.clearTimeout(pendingDelete.timer);
    setItems((prev) => sortByCreatedAtDesc([...prev, pendingDelete.item]));
    setPendingDelete(null);
    setMessage("삭제를 취소했습니다.");
  }

  return (
    <section className="saved-section">
      {message ? (
        <p className="status-text" role="status" aria-live="polite">
          {message}
          {pendingDelete ? (
            <button type="button" className="link-button" onClick={handleUndoDelete}>
              실행 취소
            </button>
          ) : null}
        </p>
      ) : null}

      {latestRound ? (
        <p className="saved-draw-ref">
          {latestRound.round}회 ({latestRound.drawDate}) 기준 당첨 결과
        </p>
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
    </section>
  );
}
