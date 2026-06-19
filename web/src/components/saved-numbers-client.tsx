"use client";

import type { FormEvent } from "react";
import { useEffect, useState, useTransition } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import { getDeviceToken } from "@/lib/device-token";
import { validateLottoNumbers } from "@/lib/lotto-validation";

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
        "X-Device-Token": getDeviceToken(),
      },
    });

    if (!response.ok) {
      const error = (await response.json()) as { message?: string };
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

    const validation = validateLottoNumbers(
      numbers.split(",").map((value) => Number(value.trim())),
    );

    if (!validation.ok) {
      setMessage(validation.message);
      return;
    }

    try {
      const response = await fetch("/api/v1/saved", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Device-Token": getDeviceToken(),
        },
        body: JSON.stringify({ numbers: validation.numbers, label: label || null, source: "MANUAL" }),
      });
      const payload = (await response.json()) as { created?: boolean; message?: string };

      if (!response.ok) {
        setMessage(payload.message ?? "저장에 실패했습니다.");
        return;
      }

      setMessage(payload.created ? "저장했습니다." : "이미 저장된 번호입니다.");
      setNumbers("");
      setLabel("");
      await loadSavedNumbers();
    } catch {
      setMessage("저장하지 못했습니다.");
    }
  }

  async function handleDelete(id: number) {
    try {
      const response = await fetch(`/api/v1/saved/${id}`, {
        method: "DELETE",
        headers: { "X-Device-Token": getDeviceToken() },
      });

      if (!response.ok) {
        setMessage("삭제에 실패했습니다.");
        return;
      }

      setMessage("삭제했습니다.");
      await loadSavedNumbers();
    } catch {
      setMessage("삭제하지 못했습니다.");
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
          저장
        </button>
      </form>

      {message ? (
        <p className="status-text" role="status" aria-live="polite">
          {message}
        </p>
      ) : null}

      <ul className="saved-list">
        {items.map((item) => (
          <li key={item.id} className="saved-item">
            <div className="saved-item-body">
              <LottoBalls numbers={item.numbers} />
              <p className="muted">{item.label ?? "메모 없음"}</p>
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
