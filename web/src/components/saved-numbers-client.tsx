"use client";

import type { FormEvent } from "react";
import { useEffect, useState, useTransition } from "react";
import { LottoBalls } from "@/components/lotto-balls";
import { getDeviceToken } from "@/lib/device-token";
import { validateLottoNumbers } from "@/lib/lotto-validation";
import type { WinningNumber } from "@/lib/api";

type EmailSubStatus = { email: string; verified: boolean } | null;

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type SavedNumber = {
  id: number;
  numbers: number[];
  label: string | null;
  source: string;
  createdAt: string;
};

const UNDO_WINDOW_MS = 5000;
const PRIZE_RANK_BY_MATCH: Record<number, string> = {
  6: "1등",
  5: "3등",
  4: "4등",
  3: "5등",
};

function sortByCreatedAtDesc(items: SavedNumber[]): SavedNumber[] {
  return [...items].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

function matchResult(numbers: number[], latest: WinningNumber): { matchCount: number; rank: string | null } {
  const matchCount = numbers.filter((value) => latest.numbers.includes(value)).length;
  const bonusMatch = numbers.includes(latest.bonusNumber);
  if (matchCount === 5 && bonusMatch) {
    return { matchCount, rank: "2등" };
  }
  return { matchCount, rank: PRIZE_RANK_BY_MATCH[matchCount] ?? null };
}

export function SavedNumbersClient() {
  const [items, setItems] = useState<SavedNumber[]>([]);
  const [latest, setLatest] = useState<WinningNumber | null>(null);
  const [label, setLabel] = useState("");
  const [numbers, setNumbers] = useState("");
  const [message, setMessage] = useState("");
  const [isPending, startTransition] = useTransition();
  const [pendingDelete, setPendingDelete] = useState<{ item: SavedNumber; timer: number } | null>(null);

  // 이메일 알림 구독 상태
  const [emailStatus, setEmailStatus] = useState<EmailSubStatus>(undefined as unknown as EmailSubStatus);
  const [emailInput, setEmailInput] = useState("");
  const [emailMessage, setEmailMessage] = useState("");
  const [emailPending, startEmailTransition] = useTransition();

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

  async function loadLatest() {
    try {
      const response = await fetch("/api/v1/rounds/latest");
      if (!response.ok) return;
      setLatest((await response.json()) as WinningNumber);
    } catch {
      // 최신 회차 대조는 부가 정보이므로 실패해도 저장 목록 표시에는 영향 없음.
    }
  }

  async function loadEmailStatus() {
    try {
      const response = await fetch("/api/v1/notifications/email", {
        headers: { "X-Device-Token": getDeviceToken() },
      });
      if (response.status === 404) { setEmailStatus(null); return; }
      if (response.ok) setEmailStatus((await response.json()) as EmailSubStatus);
    } catch {
      setEmailStatus(null);
    }
  }

  useEffect(() => {
    // URL 파라미터로 인증 완료 또는 수신 거부 결과 표시
    const params = new URLSearchParams(window.location.search);
    if (params.get("emailVerified") === "true") {
      setEmailMessage("이메일 인증이 완료되었습니다. 매주 토요일 추첨 후 알림을 보내드립니다.");
    } else if (params.get("emailUnsubscribed") === "true") {
      setEmailMessage("수신 거부가 완료되었습니다.");
    }

    startTransition(() => {
      loadSavedNumbers().catch((error: Error) => setMessage(error.message));
      loadLatest();
      void loadEmailStatus();
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

  async function finalizeDelete(item: SavedNumber) {
    try {
      const response = await fetch(`/api/v1/saved/${item.id}`, {
        method: "DELETE",
        headers: { "X-Device-Token": getDeviceToken() },
      });

      if (!response.ok) {
        setItems((previous) => sortByCreatedAtDesc([...previous, item]));
        setMessage("삭제에 실패했습니다.");
      }
    } catch {
      setItems((previous) => sortByCreatedAtDesc([...previous, item]));
      setMessage("삭제하지 못했습니다.");
    }
  }

  function handleDelete(item: SavedNumber) {
    if (pendingDelete) {
      window.clearTimeout(pendingDelete.timer);
      void finalizeDelete(pendingDelete.item);
    }

    setItems((previous) => previous.filter((existing) => existing.id !== item.id));
    const timer = window.setTimeout(() => {
      setPendingDelete(null);
      void finalizeDelete(item);
    }, UNDO_WINDOW_MS);
    setPendingDelete({ item, timer });
    setMessage("삭제했습니다.");
  }

  function handleUndoDelete() {
    if (!pendingDelete) {
      return;
    }
    window.clearTimeout(pendingDelete.timer);
    setItems((previous) => sortByCreatedAtDesc([...previous, pendingDelete.item]));
    setPendingDelete(null);
    setMessage("삭제를 취소했습니다.");
  }

  async function handleEmailSubscribe(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setEmailMessage("");
    if (!EMAIL_RE.test(emailInput)) {
      setEmailMessage("올바른 이메일 주소를 입력해 주세요.");
      return;
    }
    startEmailTransition(async () => {
      try {
        const response = await fetch("/api/v1/notifications/email", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-Device-Token": getDeviceToken(),
          },
          body: JSON.stringify({ email: emailInput }),
        });
        if (response.ok || response.status === 202) {
          setEmailMessage("인증 이메일을 발송했습니다. 메일함을 확인해 주세요.");
          setEmailInput("");
          await loadEmailStatus();
        } else {
          const payload = (await response.json()) as { message?: string };
          setEmailMessage(payload.message ?? "오류가 발생했습니다. 다시 시도해 주세요.");
        }
      } catch {
        setEmailMessage("오류가 발생했습니다. 다시 시도해 주세요.");
      }
    });
  }

  async function handleEmailUnsubscribe() {
    setEmailMessage("");
    try {
      await fetch("/api/v1/notifications/email", {
        method: "DELETE",
        headers: { "X-Device-Token": getDeviceToken() },
      });
      setEmailStatus(null);
      setEmailMessage("알림 구독이 해지되었습니다.");
    } catch {
      setEmailMessage("오류가 발생했습니다. 다시 시도해 주세요.");
    }
  }

  return (
    <section className="saved-section">
      <p className="muted">
        저장 번호는 이 기기/브라우저에 연결됩니다. 브라우저 데이터를 삭제하거나 다른 기기로 바꾸면
        이어서 볼 수 없으니 참고하세요.
      </p>

      {/* 이메일 알림 구독 섹션 */}
      <div className="email-alert-box">
        <p className="email-alert-title">🔔 당첨 알림 받기</p>
        {emailStatus === undefined ? null : emailStatus?.verified ? (
          <div>
            <p className="muted" style={{ margin: "0 0 8px" }}>
              <strong>{emailStatus.email}</strong>으로 매주 토요일 추첨 결과를 보내드립니다.
            </p>
            <button type="button" className="secondary" style={{ fontSize: "13px", padding: "6px 14px" }}
              onClick={() => { void handleEmailUnsubscribe(); }}>
              알림 해지
            </button>
          </div>
        ) : emailStatus && !emailStatus.verified ? (
          <div>
            <p className="muted" style={{ margin: "0 0 8px" }}>
              <strong>{emailStatus.email}</strong>으로 인증 메일을 보냈습니다. 메일함을 확인해 주세요.
            </p>
            <button type="button" className="secondary" style={{ fontSize: "13px", padding: "6px 14px" }}
              onClick={() => { void handleEmailUnsubscribe(); }}>
              취소
            </button>
          </div>
        ) : (
          <form className="email-alert-form" onSubmit={(e) => { void handleEmailSubscribe(e); }}>
            <input
              type="email"
              value={emailInput}
              onChange={(e) => setEmailInput(e.target.value)}
              placeholder="이메일 주소 입력"
              style={{ flex: 1 }}
            />
            <button type="submit" disabled={emailPending}>
              구독
            </button>
          </form>
        )}
        {emailMessage ? (
          <p className="status-text" role="status" aria-live="polite" style={{ margin: "8px 0 0" }}>
            {emailMessage}
          </p>
        ) : null}
      </div>
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
          {pendingDelete ? (
            <button type="button" className="link-button" onClick={handleUndoDelete}>
              실행 취소
            </button>
          ) : null}
        </p>
      ) : null}

      {latest ? (
        <p className="muted saved-latest-note">
          {latest.round}회 당첨 결과와 자동으로 대조합니다.
        </p>
      ) : null}

      <ul className="saved-list">
        {items.map((item) => {
          const currentLatest = latest;
          const result = currentLatest ? matchResult(item.numbers, currentLatest) : null;
          return (
            <li key={item.id} className="saved-item">
              <div className="saved-item-body">
                <LottoBalls numbers={item.numbers} />
                <p className="muted">{item.label ?? "메모 없음"}</p>
                {result && currentLatest ? (
                  <p className={`saved-match${result.rank ? " saved-match-win" : ""}`} role="status">
                    {currentLatest.round}회 기준 {result.matchCount}개 일치
                    {result.rank ? ` · ${result.rank}` : ""}
                  </p>
                ) : null}
              </div>
              <button type="button" className="secondary" onClick={() => handleDelete(item)}>
                삭제
              </button>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
