"use client";

import { useEffect, useState } from "react";
import { flushSync } from "react-dom";
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

  useEffect(() => {
    browserFetch<SavedNumber[]>("/api/v1/saved", {
      headers: { "X-Device-Token": getDeviceToken() },
    })
      .then(setItems)
      .catch(() => {});
  }, []);

  async function handleDelete(item: SavedNumber) {
    flushSync(() => setItems((prev) => prev.filter((x) => x.id !== item.id)));
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
    <section className="saved-section">
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
