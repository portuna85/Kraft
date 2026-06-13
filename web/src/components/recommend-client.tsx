"use client";

import { useState } from "react";

type RecommendationResponse = {
  recommendations: number[][];
};

const deviceStorageKey = "kraft-device-token";

function getDeviceToken(): string {
  const existing = window.localStorage.getItem(deviceStorageKey);
  if (existing) {
    return existing;
  }

  const created = typeof crypto.randomUUID === "function"
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  window.localStorage.setItem(deviceStorageKey, created);
  return created;
}

export function RecommendClient() {
  const [count, setCount] = useState("3");
  const [excluded, setExcluded] = useState("");
  const [recommendations, setRecommendations] = useState<number[][]>([]);
  const [message, setMessage] = useState("");
  const [savingIndex, setSavingIndex] = useState<number | null>(null);

  async function handleRecommend(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");

    const excludedNumbers = excluded
      .split(",")
      .map((value) => Number(value.trim()))
      .filter((value) => !Number.isNaN(value));

    const response = await fetch("/api/v1/numbers/recommend", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        count: Number(count),
        excludedNumbers
      })
    });

    const payload = await response.json() as RecommendationResponse | { message?: string };
    if (!response.ok) {
      setMessage("추천 번호를 생성하지 못했습니다.");
      return;
    }

    setRecommendations((payload as RecommendationResponse).recommendations);
    setMessage("추천 번호를 생성했습니다.");
  }

  async function saveRecommendation(numbers: number[], index: number) {
    setSavingIndex(index);
    setMessage("");

    const response = await fetch("/api/v1/saved", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Device-Token": getDeviceToken()
      },
      body: JSON.stringify({
        numbers,
        label: `추천 조합 ${index + 1}`,
        source: "RECOMMEND"
      })
    });

    const payload = await response.json() as { message?: string };
    setSavingIndex(null);

    if (!response.ok) {
      setMessage(payload.message ?? "추천 번호를 저장하지 못했습니다.");
      return;
    }

    setMessage(response.status === 201 ? "추천 번호를 저장했습니다." : "이미 저장된 추천 번호입니다.");
  }

  return (
    <section className="recommend-section">
      <form className="recommend-form" onSubmit={handleRecommend}>
        <label>
          추천 세트 수
          <input value={count} onChange={(event) => setCount(event.target.value)} />
        </label>
        <label>
          제외 번호
          <input
            value={excluded}
            onChange={(event) => setExcluded(event.target.value)}
            placeholder="예: 1, 2, 3"
          />
        </label>
        <button type="submit">추천 받기</button>
      </form>

      {message ? <p className="status-text">{message}</p> : null}

      <div className="recommend-grid">
        {recommendations.map((numbers, index) => (
          <article key={`${numbers.join("-")}-${index}`} className="recommend-card">
            <p className="eyebrow">추천 {index + 1}</p>
            <strong className="recommend-numbers">{numbers.join(", ")}</strong>
            <button type="button" onClick={() => saveRecommendation(numbers, index)} disabled={savingIndex === index}>
              {savingIndex === index ? "저장 중..." : "저장하기"}
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}
