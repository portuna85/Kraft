"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const links = [
  { href: "/", label: "홈" },
  { href: "/latest", label: "최신 회차" },
  { href: "/rounds", label: "회차 목록" },
  { href: "/frequency", label: "빈도" },
  { href: "/stats", label: "패턴" },
  { href: "/companion", label: "동반" },
  { href: "/analysis", label: "조합 분석" },
  { href: "/recommend", label: "번호 추천" },
  { href: "/saved", label: "저장함" },
];

export function NavLinks() {
  const pathname = usePathname();
  return (
    <nav className="nav" aria-label="주요 메뉴">
      {links.map((link) => {
        const isActive =
          link.href === "/"
            ? pathname === "/"
            : pathname === link.href || pathname.startsWith(link.href + "/");
        return (
          <Link
            key={link.href}
            href={link.href}
            aria-current={isActive ? "page" : undefined}
          >
            {link.label}
          </Link>
        );
      })}
    </nav>
  );
}
