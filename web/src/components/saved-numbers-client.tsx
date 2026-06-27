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

export function SavedNumbersClient() {
  const [items, setItems] = useState<SavedNumber[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

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

  return (
    <div className="saved-layout">
      {isLoading ? (
        <p className="saved-empty-state">저장된 번호를 불러오는 중입니다.</p>
      ) : hasError ? (
        <p className="saved-empty-state">저장 번호를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
      ) : items.length === 0 ? (
        <p className="saved-empty-state">아직 저장한 번호가 없습니다. 추천 페이지에서 조합을 저장해 보세요.</p>
      ) : (
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
      )}
    </div>
  );
}
