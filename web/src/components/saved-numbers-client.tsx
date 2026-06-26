"use client";

import { useEffect, useRef, useState } from "react";
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

const UNDO_WINDOW_MS = Number(process.env.NEXT_PUBLIC_UNDO_WINDOW_MS ?? 5000);

function sortByCreatedAtDesc(items: SavedNumber[]): SavedNumber[] {
  return [...items].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export function SavedNumbersClient() {
  const [items, setItems] = useState<SavedNumber[]>([]);
  const [message, setMessage] = useState("");
  const [pendingDelete, setPendingDelete] = useState<{ item: SavedNumber; timer: number } | null>(null);
  const pendingDeleteRef = useRef(pendingDelete);

  useEffect(() => {
    pendingDeleteRef.current = pendingDelete;
  }, [pendingDelete]);

  // Commit any in-flight delete when the component unmounts so the user's
  // confirmed action is never silently dropped.
  useEffect(() => {
    return () => {
      const pd = pendingDeleteRef.current;
      if (pd) {
        window.clearTimeout(pd.timer);
        void finalizeDelete(pd.item);
      }
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    browserFetch<SavedNumber[]>("/api/v1/saved", {
      headers: { "X-Device-Token": getDeviceToken() },
    })
      .then(setItems)
      .catch((err: unknown) => {
        setMessage(
          err instanceof BrowserApiError && err.message
            ? err.message
            : "저장한 번호를 불러오지 못했습니다.",
        );
      });
  }, []);

  async function finalizeDelete(item: SavedNumber) {
    try {
      await browserFetch(`/api/v1/saved/${item.id}`, {
        method: "DELETE",
        headers: { "X-Device-Token": getDeviceToken() },
      });
    } catch (err) {
      setItems((prev) => sortByCreatedAtDesc([...prev, item]));
      if (err instanceof BrowserApiError) {
        setMessage(err.message || "삭제에 실패했습니다.");
      } else {
        setMessage("삭제하지 못했습니다.");
      }
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

      <ul className="saved-list">
        {items.map((item) => (
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
          </li>
        ))}
      </ul>
    </section>
  );
}
