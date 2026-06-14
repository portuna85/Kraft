import type { Metadata } from "next";
import { headers } from "next/headers";
import { LottoBalls } from "@/components/lotto-balls";
import { JsonLdLottoRound } from "@/components/json-ld";
import { getLatestWinningNumber, getPublicBaseUrl } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import { calcAfterTax } from "@/lib/tax";
import type { WinningNumber } from "@/lib/api";
import logger from "@/lib/logger";

export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  let latest: WinningNumber | null = null;
  try {
    latest = await getLatestWinningNumber();
  } catch {
    // no data yet
  }
  if (!latest) {
    return {
      title: "최신 회차 로또 당첨 결과 | KRAFT Lotto",
      description: "가장 최근 로또 회차의 당첨 번호와 추첨 정보를 확인합니다.",
      alternates: { canonical: "/latest" },
    };
  }
  return {
    title: `제${latest.round}회 로또 당첨번호 (${latest.drawDate})`,
    description: `제${latest.round}회 로또 당첨 번호, 보너스 번호, 추첨일과 당첨 금액을 확인합니다.`,
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
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  let latest: WinningNumber | null = null;
  try {
    latest = await getLatestWinningNumber();
  } catch (err) {
    logger.warn({ err }, "최신 당첨번호 조회 실패 (데이터 없음)");
  }
  const baseUrl = getPublicBaseUrl();

  if (!latest) {
    return (
      <section className="panel">
        <p className="eyebrow">최신 회차</p>
        <h1 className="page-title">최신 결과를 준비하고 있습니다</h1>
        <p className="page-subtitle">아직 반영된 회차가 없습니다. 잠시 후 다시 확인해 주세요.</p>
      </section>
    );
  }

  return (
    <>
      <JsonLdLottoRound baseUrl={baseUrl} round={latest.round} drawDate={latest.drawDate} nonce={nonce} />
      <section className="panel">
        <p className="eyebrow">최신 회차</p>
        <h1 className="page-title">{latest.round}회 당첨 결과</h1>
        <p className="page-subtitle">{formatDrawDate(latest.drawDate)} 기준 추첨 정보</p>
        <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
        <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
        <p className="muted">세후 예상 수령액 {formatCurrency(calcAfterTax(latest.firstPrizeAmount))}</p>
      </section>
    </>
  );
}
