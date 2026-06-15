"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";

const primaryLinks = [
  { href: "/", label: "홈" },
  { href: "/latest", label: "최신 결과" },
  { href: "/recommend", label: "번호 추천" },
  { href: "/saved", label: "저장한 번호" },
  { href: "/rounds", label: "전체 회차" },
  { href: "/frequency", label: "출현 통계" },
];

const mobileOnlyLinks = [
  { href: "/stats", label: "패턴 통계" },
  { href: "/companion", label: "동반 출현" },
  { href: "/analysis", label: "번호 분석" },
];

function isCurrent(href: string, pathname: string): boolean {
  if (href === "/") return pathname === "/";
  return pathname === href || pathname.startsWith(href + "/");
}

export function NavLinks() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!open) return;

    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };

    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
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

  const mobileItems = [...primaryLinks, ...mobileOnlyLinks].map((link) => (
    <Link
      key={link.href}
      href={link.href}
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
        className="nav-toggle"
        onClick={() => setOpen((v) => !v)}
        aria-label={open ? "메뉴 닫기" : "메뉴 열기"}
        aria-expanded={open}
        aria-controls="nav-mobile"
      >
        <span className={`hamburger${open ? " open" : ""}`} />
      </button>

      {open && (
        <>
          <div className="nav-backdrop" aria-hidden="true" onClick={() => setOpen(false)} />
          <nav id="nav-mobile" className="nav-mobile" aria-label="주요 메뉴">
            {mobileItems}
          </nav>
        </>
      )}
    </>
  );
}
