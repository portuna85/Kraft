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
    default: "KRAFT Lotto | 로또 결과와 번호 관리",
    template: "%s | KRAFT Lotto",
  },
  description: "최신 결과 확인, 회차 조회, 번호 추천, 저장 기능을 한 곳에서 제공합니다.",
  alternates: {
    canonical: "/",
  },
  openGraph: {
    title: "KRAFT Lotto",
    description: "로또 결과 확인, 번호 추천, 저장 기능을 한 곳에서 제공합니다.",
    url: baseUrl,
    siteName: "KRAFT Lotto",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "KRAFT Lotto",
    description: "로또 결과 확인, 번호 추천, 저장 기능을 한 곳에서 제공합니다.",
  },
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const nonce = (await headers()).get("x-nonce") ?? undefined;

  return (
    <html lang="ko" className={`${notoSansKR.variable} ${notoSerifKR.variable} ${spaceGrotesk.variable}`}>
      <body>
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
