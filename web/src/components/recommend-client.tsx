"use client";

import { useEffect, useState } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import { getDeviceToken } from "@/lib/device-token";
import { parseExcludedNumbers } from "@/lib/lotto-validation";
import type { RecommendationResponse } from "@/lib/api";

export function RecommendClient() {
  const [count, setCount] = useState("5");
  const [excluded, setExcluded] = useState("");
  const [maximizePrize, setMaximizePrize] = useState(true);
  const [recommendations, setRecommendations] = useState<number[][]>([]);
  const [message, setMessage] = useState("");
  const [savingIndex, setSavingIndex] = useState<number | null>(null);
  const [savedIndexes, setSavedIndexes] = useState<Set<number>>(new Set());
  const [isPending, setIsPending] = useState(false);

  async function fetchRecommendations(
    reqCount: number,
    reqExcluded: number[],
    reqMaximizePrize: boolean,
    initialMessage = "",
  ) {
    setMessage(initialMessage);
    setIsPending(true);
    setSavedIndexes(new Set());

    try {
      const response = await fetch("/api/v1/numbers/recommend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          count: reqCount,
          excludedNumbers: reqExcluded,
          maximizePrize: reqMaximizePrize,
        }),
      });
      const payload = (await response.json()) as RecommendationResponse | { message?: string };

      if (!response.ok) {
        setMessage((payload as { message?: string }).message ?? "추천 생성에 실패했습니다.");
        return;
      }

      setRecommendations((payload as RecommendationResponse).recommendations);
    } catch {
      setMessage("추천 결과를 불러오지 못했습니다.");
    } finally {
      setIsPending(false);
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchRecommendations(5, [], true);
    }, 0);

    return () => window.clearTimeout(timer);
  }, []);

  async function handleRecommend(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const { valid: excludedNumbers, ignored } = parseExcludedNumbers(excluded);
    const ignoredMessage = ignored.length > 0 ? `무시된 입력값: ${ignored.join(", ")}` : "";
    await fetchRecommendations(Number(count), excludedNumbers, maximizePrize, ignoredMessage);
  }

  async function handleSave(numbers: number[], index: number) {
    setSavingIndex(index);
    setMessage("");

    try {
      const response = await fetch("/api/v1/saved", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Device-Token": getDeviceToken(),
        },
        body: JSON.stringify({
          numbers,
          label: `추천 조합 ${index + 1}`,
          source: "RECOMMEND",
        }),
      });
      const payload = (await response.json()) as { created?: boolean; message?: string };

      if (!response.ok) {
        setMessage(payload.message ?? "저장에 실패했습니다.");
        return;
      }

      setSavedIndexes((previous) => new Set(previous).add(index));
      setMessage(payload.created ? "저장했습니다." : "이미 저장된 조합입니다.");
    } catch {
      setMessage("저장하지 못했습니다.");
    } finally {
      setSavingIndex(null);
    }
  }

  return (
    <div className="recommend-layout">
      <form className="recommend-form" onSubmit={handleRecommend}>
        <label>
          조합 수
          <input
            type="number"
            min="1"
            max="10"
            value={count}
            onChange={(event) => setCount(event.target.value)}
          />
        </label>
        <label>
          제외 번호
          <input
            value={excluded}
            onChange={(event) => setExcluded(event.target.value)}
            placeholder="예: 1, 2, 3"
          />
        </label>
        <label className="recommend-toggle">
          <input
            type="checkbox"
            checked={maximizePrize}
            onChange={(event) => setMaximizePrize(event.target.checked)}
          />
          당첨금 우선 추천
          <span className="recommend-toggle-hint">
            과거 1등 조합을 제외하고 상대적으로 덜 겹치는 조합을 우선 선택합니다.
          </span>
        </label>
        <button type="submit" disabled={isPending}>
          {isPending ? "생성 중..." : "추천받기"}
        </button>
      </form>

      {message ? (
        <p className="status-text" role="status" aria-live="polite">
          {message}
        </p>
      ) : null}

      {recommendations.length > 0 && (
        <div className="recommend-grid">
          {recommendations.map((numbers, index) => (
            <article key={`${numbers.join("-")}-${index}`} className="recommend-card">
              <div className="recommend-card-header">
                <p className="eyebrow">추천 {index + 1}</p>
                <button
                  type="button"
                  onClick={() => handleSave(numbers, index)}
                  disabled={savingIndex === index || savedIndexes.has(index)}
                  className={`recommend-save-btn${savedIndexes.has(index) ? " saved" : ""}`}
                >
                  {savedIndexes.has(index) ? "저장됨" : savingIndex === index ? "저장 중..." : "저장"}
                </button>
              </div>
              <LottoBalls numbers={numbers} />
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
