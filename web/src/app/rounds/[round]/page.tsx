import type { Metadata } from "next";
import Link from "next/link";
import { headers } from "next/headers";
import { notFound } from "next/navigation";
import { LottoBalls } from "@/components/lotto-balls";
import { PrizeTable } from "@/components/prize-table";
import { JsonLdLottoRound } from "@/components/json-ld";
import { BackendError, getLatestWinningNumber, getPublicBaseUrl, getRound } from "@/lib/api";
import { analyzeNumbers } from "@/lib/analyze";
import { AdSenseSidebar, InArticleAd } from "@/components/ad-unit";
import { AnalysisResult } from "@/components/analysis-result";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import logger from "@/lib/logger";

type Props = {
  params: Promise<{ round: string }>;
};

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { round } = await params;
  const roundNumber = Number(round);

  if (Number.isNaN(roundNumber) || roundNumber < 1) return {};

  try {
    const data = await getRound(roundNumber);
    const balls = [...data.numbers, data.bonusNumber].join(", ");
    const baseUrl = getPublicBaseUrl();

    return {
      title: `${data.round}회 로또 당첨번호 (${data.drawDate})`,
      description: `${data.round}회 로또 6/45 당첨 번호 ${balls}. 1등 당첨금 ${formatCurrency(data.firstPrizeAmount)}.`,
      alternates: { canonical: `/rounds/${data.round}` },
      openGraph: {
        title: `${data.round}회 로또 당첨번호`,
        description: `당첨번호 ${data.numbers.join(" ")} + ${data.bonusNumber} | ${formatDrawDate(data.drawDate)}`,
        url: `/rounds/${data.round}`,
        images: [{ url: `${baseUrl}/api/og/round/${data.round}?b=${data.numbers.join(",")}&bo=${data.bonusNumber}&d=${data.drawDate}&p=${data.firstPrizeAmount}`, width: 1200, height: 630 }],
      },
    };
  } catch {
    return {};
  }
}

export default async function RoundDetailPage({ params }: Props) {
  const { round } = await params;
  const roundNumber = Number(round);

  if (Number.isNaN(roundNumber) || roundNumber < 1) {
    notFound();
  }

  const [latest, roundResult] = await Promise.all([
    getLatestWinningNumber().catch(() => null),
    getRound(roundNumber).then(
      (value) => ({ ok: true as const, value }),
      (error) => ({ ok: false as const, error }),
    ),
  ]);

  const latestRound = latest?.round ?? 0;

  let data;
  if (roundResult.ok) {
    data = roundResult.value;
  } else {
    const error = roundResult.error;
    // 존재하지 않는 회차(백엔드 404)는 진짜 404로, 백엔드 장애(5xx)나 네트워크 오류는
    // error.tsx(5xx)로 구분한다 — 이전에는 두 경우 모두 notFound()로 뭉뚱그려져
    // 백엔드 장애가 "없는 페이지"로 위장됐다.
    if (error instanceof BackendError && error.status < 500) {
      notFound();
    }
    logger.error({ err: error }, `${roundNumber}회차 조회 실패 — 핵심 데이터 실패로 페이지 오류 처리`);
    throw error;
  }

  const analysis = analyzeNumbers(data.numbers);

  const hasPrev = data.round > 1;
  const hasNext = latestRound > 0 ? data.round < latestRound : false;
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const baseUrl = getPublicBaseUrl();

  return (
    <div className="page-with-sidebar">
    <section className="panel">
      <JsonLdLottoRound
        baseUrl={baseUrl}
        round={data.round}
        drawDate={data.drawDate}
        nonce={nonce}
        analysis={analysis}
      />
      <p className="eyebrow">회차 상세</p>
      <h1 className="page-title">{data.round}회 당첨 결과</h1>
      <p className="page-subtitle">{formatDrawDate(data.drawDate)}</p>

      <div className="round-detail-balls">
        <LottoBalls numbers={data.numbers} bonusNumber={data.bonusNumber} />
      </div>

      <PrizeTable firstPrizeAmount={data.firstPrizeAmount} secondPrize={data.secondPrize} />

      <div className="round-detail-grid">
        <div className="round-detail-cell">
          <p className="round-detail-label">총 판매 금액</p>
          <p className="round-detail-value">{formatCurrency(data.totalSales)}</p>
        </div>
      </div>

      <AnalysisResult analysis={analysis} title="당첨 번호 분석" />

      <InArticleAd slot="rounds-detail" />

      <nav className="round-nav" aria-label="회차 이동">
        {hasPrev ? (
          <Link href={`/rounds/${data.round - 1}`} className="button secondary">
            {data.round - 1}회
          </Link>
        ) : (
          <span className="round-nav-slot" aria-hidden="true" />
        )}

        <Link href="/rounds" className="button secondary">
          전체 목록
        </Link>

        {hasNext ? (
          <Link href={`/rounds/${data.round + 1}`} className="button secondary">
            {data.round + 1}회
          </Link>
        ) : (
          <span className="round-nav-slot" aria-hidden="true" />
        )}
      </nav>
    </section>
      <AdSenseSidebar slot={process.env.NEXT_PUBLIC_ADSENSE_UNIT_SIDEBAR ?? ""} />
    </div>
  );
}
