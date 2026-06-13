import type { Metadata } from "next";
import { headers } from "next/headers";
import { LottoBalls } from "@/components/lotto-balls";
import { JsonLdLottoRound } from "@/components/json-ld";
import { getLatestWinningNumber, getPublicBaseUrl } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
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
      title: "최신 회차 로또 당첨번호 | KRAFT Lotto",
      description: "최신 로또 당첨 번호를 KST 기준으로 확인합니다.",
      alternates: { canonical: "/latest" },
    };
  }
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
        <h1 className="page-title">데이터 준비 중</h1>
        <p className="page-subtitle">아직 등록된 당첨번호가 없습니다. 잠시 후 다시 확인해 주세요.</p>
      </section>
    );
  }

  return (
    <>
      <JsonLdLottoRound baseUrl={baseUrl} round={latest.round} drawDate={latest.drawDate} nonce={nonce} />
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
