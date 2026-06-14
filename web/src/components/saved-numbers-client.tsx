"use client";

import type { FormEvent } from "react";
import { useEffect, useState, useTransition } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import { getDeviceToken } from "@/lib/device-token";

type SavedNumber = {
  id: number;
  numbers: number[];
  label: string | null;
  source: string;
  createdAt: string;
};

export function SavedNumbersClient() {
  const [items, setItems] = useState<SavedNumber[]>([]);
  const [label, setLabel] = useState("");
  const [numbers, setNumbers] = useState("");
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();

  async function loadSavedNumbers() {
    const response = await fetch("/api/v1/saved", {
      headers: {
        "X-Device-Token": getDeviceToken()
      }
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message ?? "저장한 번호를 불러오지 못했습니다.");
    }

    const payload = (await response.json()) as SavedNumber[];
    setItems(payload);
  }

  useEffect(() => {
    startTransition(() => {
      loadSavedNumbers().catch((error: Error) => setMessage(error.message));
    });
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    const parsedNumbers = numbers
      .split(",")
      .map((value) => Number(value.trim()))
      .filter((value) => !Number.isNaN(value));
    try {
      const response = await fetch("/api/v1/saved", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Device-Token": getDeviceToken()
        },
        body: JSON.stringify({ numbers: parsedNumbers, label: label || null, source: "MANUAL" })
      });
      const payload = await response.json() as { created?: boolean; message?: string };
      if (!response.ok) {
        setMessage(payload.message ?? "번호를 저장하지 못했습니다.");
        return;
      }
      setMessage(payload.created ? "번호를 저장 목록에 추가했습니다." : "이미 저장된 번호입니다.");
      setNumbers("");
      setLabel("");
      await loadSavedNumbers();
    } catch {
      setMessage("네트워크 오류가 발생했습니다.");
    }
  }

  async function handleDelete(id: number) {
    try {
      const response = await fetch(`/api/v1/saved/${id}`, {
        method: "DELETE",
        headers: { "X-Device-Token": getDeviceToken() }
      });
      if (!response.ok) {
        setMessage("선택한 번호를 삭제하지 못했습니다.");
        return;
      }
      setMessage("번호를 저장 목록에서 삭제했습니다.");
      await loadSavedNumbers();
    } catch {
      setMessage("네트워크 오류가 발생했습니다.");
    }
  }

  return (
    <section className="saved-section">
      <form className="saved-form" onSubmit={handleSubmit}>
        <label>
          번호 6개
          <input
            value={numbers}
            onChange={(event) => setNumbers(event.target.value)}
            placeholder="예: 3, 11, 19, 28, 34, 42"
          />
        </label>
        <label>
          메모
          <input
            value={label}
            onChange={(event) => setLabel(event.target.value)}
            placeholder="예: 자동 추천 1번"
          />
        </label>
        <button type="submit" disabled={isPending}>
          번호 저장
        </button>
      </form>

      {message ? <p className="status-text" role="status" aria-live="polite">{message}</p> : null}

      <ul className="saved-list">
        {items.map((item) => (
          <li key={item.id} className="saved-item">
            <div>
              <LottoBalls numbers={item.numbers} />
              <p className="muted">{item.label ?? "메모 없이 저장한 조합"}</p>
            </div>
            <button type="button" className="secondary" onClick={() => handleDelete(item.id)}>
              삭제
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
