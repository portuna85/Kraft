import Link from "next/link";
import { NavLinks } from "@/components/nav-links";

export function Header() {
  return (
    <header className="site-header">
      <div className="shell header-inner">
        <Link href="/" className="brand" aria-label="KRAFT Lotto 홈">
          KRAFT Lotto
        </Link>
        <NavLinks />
      </div>
    </header>
  );
}
