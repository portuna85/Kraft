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
          body: JSON.stringify({ numbers: parts })
        });
        if (!res.ok) {
          const body = await res.json();
          setError(body.message ?? "분석 요청에 실패했습니다.");
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
            placeholder="예: 3,11,19,28,34,42"
          />
        </label>
        <button type="submit" disabled={isPending}>분석</button>
      </form>

      {error ? <p className="status-text error">{error}</p> : null}

      {result ? (
        <div className="analysis-result">
          <h2 className="section-title">분석 결과</h2>
          <dl className="result-dl">
            <dt>번호</dt>
            <dd>{result.numbers.join(", ")}</dd>
            <dt>홀수 / 짝수</dt>
            <dd>{result.oddCount}개 / {result.evenCount}개</dd>
            <dt>저번호(1-22) / 고번호(23-45)</dt>
            <dd>{result.lowCount}개 / {result.highCount}개</dd>
            <dt>합계</dt>
            <dd>{result.sumOfNumbers} ({result.sumBucket} 구간)</dd>
            <dt>연속 번호 쌍</dt>
            <dd>{result.consecutivePairCount}쌍</dd>
            <dt>번호대 분포</dt>
            <dd>
              {result.rangeDistribution
                .filter((r) => r.count > 0)
                .map((r) => `${r.range}: ${r.count}개`)
                .join(", ")}
            </dd>
          </dl>
        </div>
      ) : null}
    </div>
  );
}
