import Link from "next/link";
import { NavLinks } from "@/components/nav-links";
import { ThemeToggle } from "@/components/theme-toggle";

export function Header() {
  return (
    <header className="site-header">
      <div className="shell header-inner">
        <Link href="/" className="brand" aria-label="KRAFT Lotto 홈">
          KRAFT Lotto
        </Link>
        <div className="header-actions">
          <NavLinks />
          <ThemeToggle />
        </div>
      </div>
    </header>
  );
}
