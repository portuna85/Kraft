"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

export function RoundSearchForm() {
  const router = useRouter();
  const [value, setValue] = useState("");

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const n = parseInt(value.trim(), 10);
    if (Number.isNaN(n) || n < 1) return;
    router.push(`/rounds/${n}`);
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
          onChange={(e) => setValue(e.target.value)}
          placeholder="예: 1130"
          className="round-search-input"
        />
        <button type="submit" className="button secondary">이동</button>
      </div>
    </form>
  );
}
