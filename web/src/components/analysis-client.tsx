"use client";

import { useState, useTransition, type FormEvent } from "react";
import type { AnalysisResponse } from "@/lib/api";

export function AnalysisClient() {
  const [input, setInput] = useState("");
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [error, setError] = useState("");
  const [isPending, startTransition] = useTransition();

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setResult(null);

    const parts = input.split(",").map((v) => parseInt(v.trim(), 10));
    if (parts.length !== 6 || parts.some(isNaN)) {
      setError("번호 6개를 쉼표로 구분해 입력하세요. 예: 3,11,19,28,34,42");
      return;
    }

    startTransition(async () => {
      try {
        const res = await fetch("/api/v1/stats/analysis", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ numbers: parts }),
          cache: "no-store"
        });
        if (!res.ok) {
          const body = await res.json().catch(() => ({}));
          setError((body as { message?: string }).message ?? "분석 요청에 실패했습니다.");
          return;
        }
        setResult(await res.json());
      } catch {
        setError("네트워크 오류가 발생했습니다.");
      }
    });
  }

  return (
    <div className="analysis-wrap">
      <form onSubmit={handleSubmit} className="analysis-form">
        <label>
          번호 6개 (쉼표 구분)
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="예: 3, 11, 19, 28, 34, 42"
            autoComplete="off"
          />
        </label>
        <button type="submit" disabled={isPending}>
          {isPending ? "분석 중…" : "분석"}
        </button>
      </form>

      {error ? <p className="status-text error">{error}</p> : null}

      {result ? (
        <div className="analysis-result">
          <h2 className="section-title">분석 결과</h2>

          <div className="result-grid">
            <div className="result-cell">
              <span className="result-label">홀수 / 짝수</span>
              <span className="result-value">{result.oddCount} / {result.evenCount}</span>
            </div>
            <div className="result-cell">
              <span className="result-label">저번호(1-22) / 고번호(23-45)</span>
              <span className="result-value">{result.lowCount} / {result.highCount}</span>
            </div>
            <div className="result-cell">
              <span className="result-label">합계</span>
              <span className="result-value">{result.sumOfNumbers}</span>
              <span className="result-sub">{result.sumBucket} 구간</span>
            </div>
            <div className="result-cell">
              <span className="result-label">연속 번호 쌍</span>
              <span className="result-value">{result.consecutivePairCount}쌍</span>
            </div>
          </div>

          <div>
            <p className="section-title" style={{ marginBottom: "10px" }}>번호대 분포</p>
            <ul className="range-dist-list">
              {result.rangeDistribution.map((r) => (
                <li key={r.range} className="range-dist-item">
                  <span className="range-label">{r.range}</span>
                  <div className="bar-track">
                    <div
                      className="bar-fill"
                      style={{ width: `${Math.round((r.count / 6) * 100)}%` }}
                    />
                  </div>
                  <span className="range-count">{r.count}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      ) : null}
    </div>
  );
}
