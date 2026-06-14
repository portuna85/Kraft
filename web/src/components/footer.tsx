import Link from "next/link";

const infoLinks = [
  { href: "/info/data-source", label: "데이터 출처" },
  { href: "/info/methodology", label: "분석 방법론" },
  { href: "/info/faq", label: "FAQ" },
  { href: "/info/privacy", label: "개인정보처리방침" },
  { href: "/info/terms", label: "이용약관" },
  { href: "/info/responsible-play", label: "건전한 이용" },
  { href: "/info/contact", label: "고객 문의" },
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
          당첨 결과 데이터는 동행복권 공식 API를 기준으로 제공되며, 모든 표기 시각은 KST를 따릅니다.
        </p>
        <p className="footer-copy muted">
          KRAFT Lotto는 참고용 조회·통계 서비스이며, 어떤 번호 조합의 당첨도 보장하지 않습니다.
        </p>
      </div>
    </footer>
  );
}
