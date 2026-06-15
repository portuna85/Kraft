"use client";

import { useState } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import type { RecommendationResponse } from "@/lib/api";
import { getDeviceToken } from "@/lib/device-token";
import { parseExcludedNumbers } from "@/lib/lotto-validation";

export function RecommendClient() {
  const [count, setCount] = useState("3");
  const [excluded, setExcluded] = useState("");
  const [recommendations, setRecommendations] = useState<number[][]>([]);
  const [message, setMessage] = useState("");
  const [savingIndex, setSavingIndex] = useState<number | null>(null);
  const [savedIndexes, setSavedIndexes] = useState<Set<number>>(new Set());
  const [isPending, setIsPending] = useState(false);

  async function handleRecommend(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setIsPending(true);
    setSavedIndexes(new Set());

    const { valid: excludedNumbers, ignored } = parseExcludedNumbers(excluded);
    if (ignored.length > 0) {
      setMessage(`무시된 입력값 (1-45 범위 외): ${ignored.join(", ")}`);
    }

    try {
      const res = await fetch("/api/v1/numbers/recommend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ count: Number(count), excludedNumbers }),
      });
      const payload = await res.json() as RecommendationResponse | { message?: string };
      if (!res.ok) {
        setMessage((payload as { message?: string }).message ?? "추천 생성에 실패했습니다.");
        return;
      }
      setRecommendations((payload as RecommendationResponse).recommendations);
      if (ignored.length === 0) setMessage("");
    } catch {
      setMessage("추천 결과를 불러오지 못했습니다.");
    } finally {
      setIsPending(false);
    }
  }

  async function handleSave(numbers: number[], index: number) {
    setSavingIndex(index);
    setMessage("");
    try {
      const res = await fetch("/api/v1/saved", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Device-Token": getDeviceToken(),
        },
        body: JSON.stringify({ numbers, label: `추천 조합 ${index + 1}`, source: "RECOMMEND" }),
      });
      const payload = await res.json() as { created?: boolean; message?: string };
      if (!res.ok) {
        setMessage(payload.message ?? "저장에 실패했습니다.");
        return;
      }
      setSavedIndexes((prev) => new Set(prev).add(index));
      setMessage(payload.created ? "저장했습니다." : "이미 저장된 조합입니다.");
    } catch {
      setMessage("저장하지 못했습니다.");
    } finally {
      setSavingIndex(null);
    }
  }

  return (
    <div style={{ marginTop: "24px", display: "grid", gap: "20px" }}>
      <form className="recommend-form" onSubmit={handleRecommend}>
        <label>
          조합 수
          <input
            type="number"
            min="1"
            max="10"
            value={count}
            onChange={(e) => setCount(e.target.value)}
          />
        </label>
        <label>
          제외 번호
          <input
            value={excluded}
            onChange={(e) => setExcluded(e.target.value)}
            placeholder="예: 1, 2, 3"
          />
        </label>
        <button type="submit" disabled={isPending}>
          {isPending ? "생성 중…" : "추천받기"}
        </button>
      </form>

      {message ? <p className="status-text" role="status" aria-live="polite">{message}</p> : null}

      {recommendations.length > 0 && (
        <div className="recommend-grid">
          {recommendations.map((numbers, index) => (
            <article key={`${numbers.join("-")}-${index}`} className="recommend-card">
              <p className="eyebrow">추천 {index + 1}</p>
              <LottoBalls numbers={numbers} />
              <button
                type="button"
                onClick={() => handleSave(numbers, index)}
                disabled={savingIndex === index || savedIndexes.has(index)}
                className={savedIndexes.has(index) ? "secondary" : ""}
              >
                {savedIndexes.has(index)
                  ? "저장됨"
                  : savingIndex === index
                  ? "저장 중…"
                  : "저장"}
              </button>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
