import Link from "next/link";

const infoLinks = [
  { href: "/info/data-source", label: "데이터 출처" },
  { href: "/info/methodology", label: "분석 방법론" },
  { href: "/info/faq", label: "FAQ" },
  { href: "/info/privacy", label: "개인정보처리방침" },
  { href: "/info/terms", label: "이용약관" },
  { href: "/info/responsible-play", label: "건전한 이용" },
  { href: "/info/contact", label: "문의하기" },
];

export function Footer() {
  return (
    <footer className="site-footer-bottom">
      <div className="shell footer-inner">
        <nav className="footer-nav">
          {infoLinks.map((link) => (
            <Link key={link.href} href={link.href}>
              {link.label}
            </Link>
          ))}
        </nav>
        <p className="footer-copy">
          당첨 번호 데이터 출처: 동행복권 공식 API &nbsp;·&nbsp; 모든 시간은 KST 기준
        </p>
        <p className="footer-copy muted">
          KRAFT Lotto는 통계 정보 제공 서비스입니다. 당첨을 보장하지 않습니다.
        </p>
      </div>
    </footer>
  );
}
