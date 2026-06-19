import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { LottoBalls } from "@/components/lotto-balls";
import { PrizeTable } from "@/components/prize-table";
import { getLatestWinningNumber, getRound } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";

export const revalidate = 3600;

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

    return {
      title: `${data.round}회 로또 당첨번호 (${data.drawDate})`,
      description: `${data.round}회 로또 6/45 당첨 번호 ${balls}. 1등 당첨금 ${formatCurrency(data.firstPrizeAmount)}.`,
      alternates: { canonical: `/rounds/${data.round}` },
      openGraph: {
        title: `${data.round}회 로또 당첨번호`,
        description: `당첨번호 ${data.numbers.join(" ")} + ${data.bonusNumber} | ${formatDrawDate(data.drawDate)}`,
        url: `/rounds/${data.round}`,
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

  let latestRound = 0;

  try {
    const latest = await getLatestWinningNumber();
    latestRound = latest.round;
  } catch {
    // ignore latest round fallback
  }

  const data = await getRound(roundNumber).catch(() => null);
  if (!data) notFound();

  const hasPrev = data.round > 1;
  const hasNext = latestRound > 0 ? data.round < latestRound : false;

  return (
    <section className="panel">
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
  );
}
