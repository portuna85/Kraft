import type { Metadata } from "next";
import { cache } from "react";
import { headers } from "next/headers";
import { LottoBalls } from "@/components/lotto-balls";
import { JsonLdLottoRound } from "@/components/json-ld";
import { getLatestWinningNumber, getPublicBaseUrl, type WinningNumber } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import { calcAfterTax } from "@/lib/tax";
import logger from "@/lib/logger";
export const revalidate = 60;

const getCachedLatest = cache(getLatestWinningNumber);

export async function generateMetadata(): Promise<Metadata> {
  let latest: WinningNumber | null = null;

  try {
    latest = await getCachedLatest();
  } catch {
    // no data yet
  }

  if (!latest) {
    return {
      title: "최신 로또 결과 | KRAFT Lotto",
      description: "최신 로또 6/45 당첨 번호와 당첨금을 확인합니다. 동행복권 공식 데이터 기반.",
      alternates: { canonical: "/latest" },
    };
  }

  return {
    title: `${latest.round}회 로또 당첨번호 (${latest.drawDate})`,
    description: `${latest.round}회 로또 6/45 당첨 번호 ${[...latest.numbers].join(", ")}(보너스 ${latest.bonusNumber}). 당첨금과 세후 예상 수령액을 확인합니다.`,
    alternates: { canonical: "/latest" },
    openGraph: {
      title: `${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      url: "/latest",
    },
  };
}

export default async function LatestPage() {
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  let latest: WinningNumber | null = null;

  try {
    latest = await getCachedLatest();
  } catch (err) {
    logger.warn({ err }, "최신 당첨번호 조회 실패");
  }

  const baseUrl = getPublicBaseUrl();

  if (!latest) {
    return (
      <section className="panel">
        <p className="eyebrow">최신 결과</p>
        <h1 className="page-title">최신 결과를 준비 중입니다</h1>
        <p className="page-subtitle">잠시 후 다시 확인해 주세요.</p>
      </section>
    );
  }

  return (
    <>
      <JsonLdLottoRound baseUrl={baseUrl} round={latest.round} drawDate={latest.drawDate} nonce={nonce} />
      <section className="panel">
        <p className="eyebrow">최신 결과</p>
        <h1 className="page-title">{latest.round}회 당첨 결과</h1>
        <p className="page-subtitle">{formatDrawDate(latest.drawDate)}</p>
        <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
        <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
        <p className="muted">세후 예상 수령액 {formatCurrency(calcAfterTax(latest.firstPrizeAmount))}</p>
      </section>
    </>
  );
}
