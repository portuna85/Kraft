"use client";

import { useEffect, useState } from "react";

const STORAGE_KEY = "kraft-theme";

function readTheme(): boolean {
  if (typeof document === "undefined") return false;
  return document.documentElement.dataset.theme === "dark";
}

export function ThemeToggle() {
  // 서버는 항상 false로 렌더링하므로(document 없음), 초기 클라이언트 렌더도 동일하게
  // false로 시작해 hydration mismatch를 피한다. mount 후 실제 테마로 한 번 갱신한다.
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    function syncTheme() {
      setIsDark(readTheme());
    }

    syncTheme();
    window.addEventListener("storage", syncTheme);
    return () => window.removeEventListener("storage", syncTheme);
  }, []);

  function toggle() {
    const next = !isDark;
    setIsDark(next);

    if (next) {
      document.documentElement.setAttribute("data-theme", "dark");
      localStorage.setItem(STORAGE_KEY, "dark");
      return;
    }

    document.documentElement.removeAttribute("data-theme");
    localStorage.setItem(STORAGE_KEY, "light");
  }

  return (
    <button
      type="button"
      onClick={toggle}
      className="theme-toggle"
      aria-label={isDark ? "라이트 모드로 전환" : "다크 모드로 전환"}
      aria-pressed={isDark}
    >
      {isDark ? (
        <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor" aria-hidden="true">
          <path d="M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.4 5.4 0 0 1-7.54-7.54A9 9 0 0 0 12 3Z" />
        </svg>
      ) : (
        <svg
          viewBox="0 0 24 24"
          width="16"
          height="16"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
        </svg>
      )}
    </button>
  );
}
