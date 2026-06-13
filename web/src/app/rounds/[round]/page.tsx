import Link from "next/link";
import { notFound } from "next/navigation";
import { LottoBalls } from "@/components/lotto-balls";
import { getRound } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";

export const revalidate = 3600;

type Props = {
  params: Promise<{
    round: string;
  }>;
};

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
        <h1 className="page-title">{data.round}회</h1>
        <p className="page-subtitle">{formatDrawDate(data.drawDate)} 추첨 결과</p>

        <div style={{ marginTop: "24px" }}>
          <LottoBalls numbers={data.numbers} bonusNumber={data.bonusNumber} />
        </div>

        <div className="round-detail-grid">
          <div className="round-detail-cell">
            <p className="round-detail-label">1등 당첨금</p>
            <p className="round-detail-value">{formatCurrency(data.firstPrizeAmount)}</p>
          </div>
          <div className="round-detail-cell">
            <p className="round-detail-label">1등 총 당첨금</p>
            <p className="round-detail-value">{formatCurrency(data.firstAccumAmount)}</p>
          </div>
          <div className="round-detail-cell">
            <p className="round-detail-label">2등 당첨금</p>
            <p className="round-detail-value">{formatCurrency(data.secondPrize)}</p>
            {data.secondWinners > 0 && (
              <p className="muted">{data.secondWinners.toLocaleString()}명</p>
            )}
          </div>
          <div className="round-detail-cell">
            <p className="round-detail-label">총 판매금액</p>
            <p className="round-detail-value">{formatCurrency(data.totalSales)}</p>
          </div>
        </div>

        <div style={{ marginTop: "24px" }}>
          <Link href="/rounds" className="button secondary">← 회차 목록</Link>
        </div>
      </section>
    );
  } catch {
    notFound();
  }
}
