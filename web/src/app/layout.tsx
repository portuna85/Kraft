import type { Metadata, Viewport } from "next";
import { Noto_Sans_KR, Noto_Serif_KR, Space_Grotesk } from "next/font/google";
import { headers } from "next/headers";
import { Footer } from "@/components/footer";
import { Header } from "@/components/header";
import { JsonLdWebSite } from "@/components/json-ld";
import { getPublicBaseUrl } from "@/lib/api";
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
};

export const metadata: Metadata = {
  metadataBase: new URL(baseUrl),
  title: {
    default: "KRAFT Lotto — 로또 6/45 당첨 결과 · 번호 추천",
    template: "%s | KRAFT Lotto",
  },
  description: "로또 6/45 최신 당첨 번호와 당첨금 조회, 회차별 통계, 무작위 번호 추천·저장을 무료로 제공합니다. 동행복권 공식 데이터 기반.",
  alternates: {
    canonical: "/",
  },
  openGraph: {
    title: "KRAFT Lotto — 로또 6/45 당첨 결과 · 번호 추천",
    description: "최신 로또 당첨 번호·당첨금 조회, 출현 통계, 무작위 번호 추천·저장. 동행복권 공식 데이터 기반.",
    url: baseUrl,
    siteName: "KRAFT Lotto",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "KRAFT Lotto — 로또 6/45 당첨 결과 · 번호 추천",
    description: "최신 로또 당첨 번호·당첨금 조회, 출현 통계, 무작위 번호 추천·저장. 동행복권 공식 데이터 기반.",
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
          dangerouslySetInnerHTML={{
            __html:
              "try{if(localStorage.getItem('kraft-theme')==='dark'){document.documentElement.setAttribute('data-theme','dark');}}catch(e){}",
          }}
        />
        <JsonLdWebSite baseUrl={baseUrl} nonce={nonce} />
        <a href="#main-content" className="skip-nav">본문으로 건너뛰기</a>
        <Header />
        <main id="main-content" className="page">
          <div className="shell">{children}</div>
        </main>
        <Footer />
      </body>
    </html>
  );
}
