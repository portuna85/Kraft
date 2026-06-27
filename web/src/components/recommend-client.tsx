"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { getDeviceToken } from "@/lib/device-token";
import { parseExcludedNumbers } from "@/lib/lotto-validation";
import type { RecommendationResponse } from "@/lib/api";
import { browserFetch, BrowserApiError } from "@/lib/browser-api";

const DEFAULT_COUNT = 5;
const DEFAULT_MAXIMIZE_PRIZE = true;

const TEXT = {
  countLabel: "\uC870\uD569 \uC218",
  excludedLabel: "\uC81C\uC678 \uBC88\uD638",
  excludedPlaceholder: "\uC608: 1, 2, 3",
  maximizePrizeLabel: "\uACF5\uB3D9 \uB2F9\uCCA8 \uBD84\uC0B0\uD615 \uCD94\uCC9C",
  maximizePrizeHint:
    "\uACFC\uAC70 1\uB4F1 \uC870\uD569\uC744 \uC81C\uC678\uD558\uACE0 \uC0C1\uB300\uC801\uC73C\uB85C \uB35C \uACB9\uCE58\uB294 \uC870\uD569\uC744 \uC6B0\uC120 \uC120\uD0DD\uD569\uB2C8\uB2E4.",
  submit: "\uCD94\uCC9C\uBC1B\uAE30",
  pending: "\uC0DD\uC131 \uC911...",
  disclaimer:
    "\uBAA8\uB4E0 6\uAC1C \uBC88\uD638 \uC870\uD569\uC758 1\uB4F1 \uB2F9\uCCA8 \uD655\uB960\uC740 \uB3D9\uC77C\uD569\uB2C8\uB2E4. \u201C\uACF5\uB3D9 \uB2F9\uCCA8 \uBD84\uC0B0\uD615 \uCD94\uCC9C\u201D\uC740 \uACF5\uB3D9 \uB2F9\uCCA8 \uAC00\uB2A5\uC131\uC744 \uB0AE\uCD94\uB294 \uC120\uD0DD\uC77C \uBF44 \uD655\uB960\uC744 \uB192\uC774\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.",
  detailLink: "\uC790\uC138\uD788 \uBCF4\uAE30",
  recommendPrefix: "\uCD94\uCC9C",
  save: "\uC800\uC7A5",
  saving: "\uC800\uC7A5 \uC911...",
  saved: "\uC800\uC7A5\uB428",
  savedCreated: "\uC800\uC7A5\uD588\uC2B5\uB2C8\uB2E4.",
  savedExists: "\uC774\uBBF8 \uC800\uC7A5\uD55C \uC870\uD569\uC785\uB2C8\uB2E4.",
  saveFailed: "\uC800\uC7A5\uD558\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.",
  generateFailed: "\uCD94\uCC9C \uC0DD\uC131\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.",
  loadFailed: "\uCD94\uCC9C \uACB0\uACFC\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.",
  ignoredPrefix: "\uBB34\uC2DC\uB41C \uC785\uB825\uAC12:",
  saveLabelPrefix: "\uCD94\uCC9C \uC870\uD569",
} as const;

export function RecommendClient() {
  const [count, setCount] = useState(String(DEFAULT_COUNT));
  const [excluded, setExcluded] = useState("");
  const [maximizePrize, setMaximizePrize] = useState(DEFAULT_MAXIMIZE_PRIZE);
  const [recommendations, setRecommendations] = useState<number[][]>([]);
  const [message, setMessage] = useState("");
  const [savingIndex, setSavingIndex] = useState<number | null>(null);
  const [savedIndexes, setSavedIndexes] = useState<Set<number>>(new Set());
  const [isPending, setIsPending] = useState(false);
  const fetchSeqRef = useRef(0);

  async function fetchRecommendations(
    reqCount: number,
    reqExcluded: number[],
    reqMaximizePrize: boolean,
    initialMessage = "",
  ) {
    const seq = ++fetchSeqRef.current;
    setMessage(initialMessage);
    setIsPending(true);
    setSavedIndexes(new Set());

    try {
      const payload = await browserFetch<RecommendationResponse>("/api/v1/numbers/recommend", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          count: reqCount,
          excludedNumbers: reqExcluded,
          maximizePrize: reqMaximizePrize,
        }),
      });
      if (seq !== fetchSeqRef.current) return;
      setRecommendations(payload.recommendations);
    } catch (err) {
      if (seq !== fetchSeqRef.current) return;
      if (err instanceof BrowserApiError) {
        setMessage(err.message || TEXT.generateFailed);
      } else {
        setMessage(TEXT.loadFailed);
      }
    } finally {
      if (seq === fetchSeqRef.current) {
        setIsPending(false);
      }
    }
  }

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchRecommendations(DEFAULT_COUNT, [], DEFAULT_MAXIMIZE_PRIZE);
    }, 0);

    return () => window.clearTimeout(timer);
  }, []);

  async function handleRecommend(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const { valid: excludedNumbers, ignored } = parseExcludedNumbers(excluded);
    const ignoredMessage =
      ignored.length > 0 ? `${TEXT.ignoredPrefix} ${ignored.join(", ")}` : "";
    await fetchRecommendations(Number(count), excludedNumbers, maximizePrize, ignoredMessage);
  }

  async function handleSave(numbers: number[], index: number) {
    setSavingIndex(index);
    setMessage("");

    try {
      const payload = await browserFetch<{ created?: boolean }>("/api/v1/saved", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Device-Token": getDeviceToken(),
        },
        body: JSON.stringify({
          numbers,
          label: `${TEXT.saveLabelPrefix} ${index + 1}`,
          source: "RECOMMEND",
        }),
      });
      setSavedIndexes((previous) => new Set(previous).add(index));
      setMessage(payload.created ? TEXT.savedCreated : TEXT.savedExists);
    } catch (err) {
      setMessage(
        err instanceof BrowserApiError && err.message ? err.message : TEXT.saveFailed,
      );
    } finally {
      setSavingIndex(null);
    }
  }

  return (
    <div className="recommend-layout">
      <form className="recommend-form" onSubmit={handleRecommend}>
        <label>
          {TEXT.countLabel}
          <input
            type="number"
            min="1"
            max="10"
            value={count}
            onChange={(event) => setCount(event.target.value)}
          />
        </label>
        <label>
          {TEXT.excludedLabel}
          <input
            type="text"
            value={excluded}
            onChange={(event) => setExcluded(event.target.value)}
            placeholder={TEXT.excludedPlaceholder}
          />
        </label>
        <label className="recommend-toggle">
          <span className="recommend-toggle-control">
            <input
              type="checkbox"
              checked={maximizePrize}
              onChange={(event) => setMaximizePrize(event.target.checked)}
            />
            <span className="recommend-toggle-label">{TEXT.maximizePrizeLabel}</span>
          </span>
          <span className="recommend-toggle-hint">{TEXT.maximizePrizeHint}</span>
        </label>
        <button type="submit" disabled={isPending}>
          {isPending ? TEXT.pending : TEXT.submit}
        </button>
        <p className="muted recommend-disclaimer">
          {TEXT.disclaimer} <Link href="/info/faq">{TEXT.detailLink}</Link>
        </p>
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
              <div className="recommend-card-row">
                <div className="recommend-card-info">
                  <p className="eyebrow">
                    {TEXT.recommendPrefix} {index + 1}
                  </p>
                  <LottoBalls numbers={numbers} />
                </div>
                <button
                  type="button"
                  onClick={() => handleSave(numbers, index)}
                  disabled={savingIndex === index || savedIndexes.has(index)}
                  className={`recommend-save-btn${savedIndexes.has(index) ? " saved" : ""}`}
                >
                  {savedIndexes.has(index)
                    ? TEXT.saved
                    : savingIndex === index
                      ? TEXT.saving
                      : TEXT.save}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
