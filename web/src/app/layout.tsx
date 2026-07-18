import type { Metadata, Viewport } from "next";
import { headers } from "next/headers";
import { Noto_Sans_KR, Noto_Serif_KR, Space_Grotesk } from "next/font/google";
import { Footer } from "@/components/footer";
import { Header } from "@/components/header";
import { StickyMobileAd } from "@/components/ad-unit";
import { JsonLdWebSite } from "@/components/json-ld";
import { getPublicBaseUrl } from "@/lib/api";
import { THEME_INIT_SCRIPT } from "@/lib/csp-inline-scripts";
import "./globals.css";

const notoSansKR = Noto_Sans_KR({
  subsets: ["latin"],
  weight: ["400", "700", "800"],
  display: "swap",
  variable: "--font-sans",
});

const notoSerifKR = Noto_Serif_KR({
  subsets: ["latin"],
  weight: ["500", "700"],
  display: "swap",
  variable: "--font-display",
});

const spaceGrotesk = Space_Grotesk({
  subsets: ["latin"],
  weight: ["500", "700"],
  display: "swap",
  variable: "--font-accent",
});

const baseUrl = getPublicBaseUrl();

export const viewport: Viewport = {
  themeColor: "#c94f24",
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export const metadata: Metadata = {
  metadataBase: new URL(baseUrl),
  title: {
    default: "KRAFT Lotto | 로또 6/45 결과와 번호 추천",
    template: "%s | KRAFT Lotto",
  },
  description: "로또 6/45 최신 당첨 번호 조회, 회차 검색, 통계, 번호 추천 기능을 제공합니다.",
  alternates: {
    canonical: "/",
  },
  openGraph: {
    title: "KRAFT Lotto | 로또 6/45 결과와 번호 추천",
    description: "최신 로또 당첨 번호 조회, 출현 통계, 패턴 분석, 번호 추천 기능을 제공합니다.",
    url: baseUrl,
    siteName: "KRAFT Lotto",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "KRAFT Lotto | 로또 6/45 결과와 번호 추천",
    description: "최신 로또 당첨 번호 조회, 출현 통계, 패턴 분석, 번호 추천 기능을 제공합니다.",
  },
  verification: {
    google: process.env.NEXT_PUBLIC_GOOGLE_SITE_VERIFICATION,
    other: process.env.NEXT_PUBLIC_NAVER_SITE_VERIFICATION
      ? { "naver-site-verification": process.env.NEXT_PUBLIC_NAVER_SITE_VERIFICATION }
      : undefined,
  },
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const nonce = (await headers()).get("x-nonce") ?? undefined;

  return (
    <html lang="ko" className={`${notoSansKR.variable} ${notoSerifKR.variable} ${spaceGrotesk.variable}`}>
      <body>
        <script
          nonce={nonce}
          suppressHydrationWarning
          dangerouslySetInnerHTML={{ __html: THEME_INIT_SCRIPT }}
        />
        <JsonLdWebSite baseUrl={baseUrl} nonce={nonce} />
        <a href="#main-content" className="skip-nav">본문으로 건너뛰기</a>
        <Header />
        <main id="main-content" className="page">
          <div className="shell">{children}</div>
        </main>
        <Footer />
        <StickyMobileAd unit={process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_STICKY ?? ""} />
      </body>
    </html>
  );
}
