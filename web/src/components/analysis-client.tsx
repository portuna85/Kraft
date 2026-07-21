"use client";

import { useState, type FormEvent } from "react";
import type { AnalysisResponse } from "@/lib/api";
import { analyzeNumbers } from "@/lib/analyze";
import { AnalysisResult } from "@/components/analysis-result";

export function AnalysisClient() {
  const [input, setInput] = useState("");
  const [result, setResult] = useState<AnalysisResponse | null>(null);
  const [error, setError] = useState("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setResult(null);

    const parts = input.split(",").map((value) => Number.parseInt(value.trim(), 10));

    if (parts.length !== 6 || parts.some(Number.isNaN)) {
      setError("번호 6개를 입력해 주세요. 예: 3, 11, 19, 28, 34, 42");
      return;
    }

    if (parts.some((n) => n < 1 || n > 45)) {
      setError("번호는 1부터 45 사이여야 합니다.");
      return;
    }

    if (new Set(parts).size !== parts.length) {
      setError("중복된 번호가 있습니다.");
      return;
    }

    setResult(analyzeNumbers(parts));
  }

  return (
    <div className="analysis-layout">
      <form onSubmit={handleSubmit} className="analysis-form">
        <label>
          번호 6개
          <input
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="예: 3, 11, 19, 28, 34, 42"
            autoComplete="off"
          />
        </label>
        <button type="submit">분석하기</button>
      </form>

      {error ? (
        <p className="status-text error" role="alert" aria-live="assertive">
          {error}
        </p>
      ) : null}

      {result ? <AnalysisResult analysis={result} title="분석 결과" /> : null}
    </div>
  );
}
