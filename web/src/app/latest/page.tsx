import type { Metadata } from "next";
import { LottoBalls } from "@/components/lotto-balls";
import { JsonLdLottoRound } from "@/components/json-ld";
import { getLatestWinningNumber, getPublicBaseUrl } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";

export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  const latest = await getLatestWinningNumber();
  return {
    title: `제${latest.round}회 로또 당첨번호 (${latest.drawDate})`,
    description: `제${latest.round}회 로또 당첨 번호와 보너스 번호, 추첨일을 KST 기준으로 확인합니다.`,
    alternates: {
      canonical: "/latest"
    },
    openGraph: {
      title: `제${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      url: "/latest"
    }
  };
}

export default async function LatestPage() {
  const latest = await getLatestWinningNumber();
  const baseUrl = getPublicBaseUrl();

  return (
    <>
      <JsonLdLottoRound baseUrl={baseUrl} round={latest.round} drawDate={latest.drawDate} />
      <section className="panel">
        <p className="eyebrow">최신 회차</p>
        <h1 className="page-title">{latest.round}회 로또 번호</h1>
        <p className="page-subtitle">{formatDrawDate(latest.drawDate)} 추첨 결과</p>
        <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
        <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
      </section>
    </>
  );
}
