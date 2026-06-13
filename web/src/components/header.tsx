import Link from "next/link";

const links = [
  { href: "/", label: "홈" },
  { href: "/latest", label: "최신 회차" },
  { href: "/rounds", label: "회차 목록" },
  { href: "/recommend", label: "번호 추천" },
  { href: "/saved", label: "저장함" }
];

export function Header() {
  return (
    <header className="site-header">
      <div className="shell header-inner">
        <Link href="/" className="brand">
          KRAFT Lotto
        </Link>
        <nav className="nav">
          {links.map((link) => (
            <Link key={link.href} href={link.href}>
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  );
}
