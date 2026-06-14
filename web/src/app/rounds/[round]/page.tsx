import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { LottoBalls } from "@/components/lotto-balls";
import { getRound } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import { calcAfterTax } from "@/lib/tax";

export const revalidate = 3600;

type Props = {
  params: Promise<{
    round: string;
  }>;
};

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { round } = await params;
  const roundNumber = Number(round);
  if (Number.isNaN(roundNumber) || roundNumber < 1) return {};
  try {
    const data = await getRound(roundNumber);
    const balls = [...data.numbers, data.bonusNumber].join(", ");
    return {
      title: `제${data.round}회 로또 당첨번호 (${data.drawDate})`,
      description: `제${data.round}회 로또 6/45 당첨번호: ${balls}. 1등 당첨금 ${formatCurrency(data.firstPrizeAmount)}`,
      alternates: { canonical: `/rounds/${data.round}` },
      openGraph: {
        title: `제${data.round}회 로또 당첨번호`,
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

  try {
    const data = await getRound(roundNumber);
    return (
      <section className="panel">
        <p className="eyebrow">회차 상세</p>
        <h1 className="page-title">{data.round}회 당첨 결과</h1>
        <p className="page-subtitle">{formatDrawDate(data.drawDate)} 추첨 기준</p>

        <div style={{ marginTop: "24px" }}>
          <LottoBalls numbers={data.numbers} bonusNumber={data.bonusNumber} />
        </div>

        <div className="round-detail-grid">
          <div className="round-detail-cell">
            <p className="round-detail-label">1등 당첨금</p>
            <p className="round-detail-value">{formatCurrency(data.firstPrizeAmount)}</p>
          </div>
          <div className="round-detail-cell">
            <p className="round-detail-label">1등 총 당첨액</p>
            <p className="round-detail-value">{formatCurrency(data.firstAccumAmount)}</p>
          </div>
          <div className="round-detail-cell">
            <p className="round-detail-label">2등 당첨금</p>
            <p className="round-detail-value">{formatCurrency(data.secondPrize)}</p>
            {data.secondWinners > 0 && (
              <p className="muted">당첨자 {data.secondWinners.toLocaleString()}명</p>
            )}
          </div>
          <div className="round-detail-cell">
            <p className="round-detail-label">총 판매금액</p>
            <p className="round-detail-value">{formatCurrency(data.totalSales)}</p>
          </div>
          <div className="round-detail-cell">
            <p className="round-detail-label">세후 예상 수령액</p>
            <p className="round-detail-value">{formatCurrency(calcAfterTax(data.firstPrizeAmount))}</p>
          </div>
        </div>

        <div style={{ marginTop: "24px" }}>
          <Link href="/rounds" className="button secondary">← 전체 회차로 돌아가기</Link>
        </div>
      </section>
    );
  } catch {
    notFound();
  }
}
