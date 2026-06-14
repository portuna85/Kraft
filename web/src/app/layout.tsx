import type { Metadata, Viewport } from "next";
import { Noto_Sans_KR } from "next/font/google";
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

const baseUrl = getPublicBaseUrl();

export const viewport: Viewport = {
  themeColor: "#c94f24",
  width: "device-width",
  initialScale: 1,
};

export const metadata: Metadata = {
  metadataBase: new URL(baseUrl),
  title: {
    default: "KRAFT Lotto | 로또 당첨 결과와 번호 관리",
    template: "%s | KRAFT Lotto"
  },
  description: "최신 로또 당첨 결과 조회, 회차별 기록 확인, 번호 추천과 저장까지 한 곳에서 관리하세요.",
  alternates: {
    canonical: "/"
  },
  openGraph: {
    title: "KRAFT Lotto",
    description: "당첨 결과 조회부터 번호 추천과 저장까지, 로또 확인에 필요한 기능을 한 곳에 모았습니다.",
    url: baseUrl,
    siteName: "KRAFT Lotto",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "KRAFT Lotto",
    description: "당첨 결과 조회부터 번호 추천과 저장까지, 로또 확인에 필요한 기능을 한 곳에 모았습니다."
  }
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  return (
    <html lang="ko" className={notoSansKR.variable}>
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
