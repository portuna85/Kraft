import { notFound } from "next/navigation";
import { LottoBalls } from "@/components/lotto-balls";
import { getRound } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";

export const dynamic = "force-dynamic";

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
        <LottoBalls numbers={data.numbers} bonusNumber={data.bonusNumber} />
        <p className="muted">1등 당첨금 {formatCurrency(data.firstPrizeAmount)}</p>
      </section>
    );
  } catch {
    notFound();
  }
}
