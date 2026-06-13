import type { Metadata } from "next";
import { headers } from "next/headers";
import { Footer } from "@/components/footer";
import { Header } from "@/components/header";
import { JsonLdWebSite } from "@/components/json-ld";
import { getPublicBaseUrl } from "@/lib/api";
import "./globals.css";

const baseUrl = getPublicBaseUrl();

export const metadata: Metadata = {
  metadataBase: new URL(baseUrl),
  title: {
    default: "KRAFT Lotto | 한국 로또 번호 조회",
    template: "%s | KRAFT Lotto"
  },
  description: "KST 기준 최신 로또 회차, 번호 추천, 저장함을 제공하는 KRAFT Lotto입니다.",
  alternates: {
    canonical: "/"
  },
  openGraph: {
    title: "KRAFT Lotto",
    description: "최신 회차 조회와 번호 추천, 저장함까지 한 번에 확인합니다.",
    url: baseUrl,
    siteName: "KRAFT Lotto",
    locale: "ko_KR",
    type: "website",
  },
  twitter: {
    card: "summary_large_image",
    title: "KRAFT Lotto",
    description: "최신 회차 조회와 번호 추천, 저장함까지 한 번에 확인합니다."
  }
};

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  return (
    <html lang="ko">
      <body>
        <JsonLdWebSite baseUrl={baseUrl} nonce={nonce} />
        <Header />
        <main className="page">
          <div className="shell">{children}</div>
        </main>
        <Footer />
      </body>
    </html>
  );
}
