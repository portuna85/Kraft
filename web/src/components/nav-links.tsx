"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

// 과거 패턴/동반/분석은 mobileOnlyLinks로 분리되어 데스크톱에서 보이지 않았다.
// 통계 페이지 발견성을 위해 데스크톱/모바일 모두 동일한 링크 목록을 노출한다.
const primaryLinks = [
  { href: "/", label: "홈" },
  { href: "/rounds", label: "회차 결과" },
  { href: "/recommend", label: "번호 추천" },
  { href: "/saved", label: "저장 번호" },
  { href: "/frequency", label: "출현 통계" },
  { href: "/stats", label: "패턴 통계" },
  { href: "/companion", label: "동반 출현" },
  { href: "/analysis", label: "번호 분석" },
];

function isCurrent(href: string, pathname: string): boolean {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function NavLinks() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const toggleRef = useRef<HTMLButtonElement>(null);
  const mobileNavRef = useRef<HTMLElement>(null);

  const closeAndReturnFocus = () => {
    setOpen(false);
    toggleRef.current?.focus();
  };

  useEffect(() => {
    if (!open) return;

    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        closeAndReturnFocus();
        return;
      }
      if (event.key !== "Tab" || !mobileNavRef.current) return;

      const focusable = mobileNavRef.current.querySelectorAll<HTMLElement>("a[href]");
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    };

    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  useEffect(() => {
    if (!open) {
      document.body.style.overflow = "";
      return;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    mobileNavRef.current?.querySelector<HTMLElement>("a[href]")?.focus();

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  const desktopItems = primaryLinks.map((link) => (
    <Link
      key={link.href}
      href={link.href}
      aria-current={isCurrent(link.href, pathname) ? "page" : undefined}
    >
      {link.label}
    </Link>
  ));

  const mobileItems = primaryLinks.map((link) => (
    <Link
      key={link.href}
      href={link.href}
      onClick={() => setOpen(false)}
      aria-current={isCurrent(link.href, pathname) ? "page" : undefined}
    >
      {link.label}
    </Link>
  ));

  return (
    <>
      <nav className="nav nav-desktop" aria-label="주요 메뉴">
        {desktopItems}
      </nav>

      <button
        ref={toggleRef}
        type="button"
        className="nav-toggle"
        onClick={() => setOpen((value) => !value)}
        aria-label={open ? "메뉴 닫기" : "메뉴 열기"}
        aria-expanded={open}
        aria-controls="nav-mobile"
      >
        <span className={`hamburger${open ? " open" : ""}`} />
      </button>

      {open && (
        <>
          <div className="nav-backdrop" aria-hidden="true" onClick={closeAndReturnFocus} />
          <div className="nav-mobile-wrap">
            <nav id="nav-mobile" ref={mobileNavRef} className="nav-mobile" aria-label="주요 메뉴">
              {mobileItems}
            </nav>
          </div>
        </>
      )}
    </>
  );
}
