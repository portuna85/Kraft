"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export function RoundSearchForm() {
  const router = useRouter();
  const [value, setValue] = useState("");

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    const round = Number.parseInt(value.trim(), 10);

    if (Number.isNaN(round) || round < 1) return;

    router.push(`/rounds/${round}`);
  }

  return (
    <form onSubmit={handleSubmit} className="round-search-form">
      <label className="round-search-label" htmlFor="round-search-input">
        회차 번호로 바로 이동
      </label>
      <div className="round-search-row">
        <input
          id="round-search-input"
          type="number"
          min="1"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="예: 1130"
          className="round-search-input"
        />
        <button type="submit" className="button secondary">
          이동
        </button>
      </div>
    </form>
  );
}
