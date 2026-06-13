import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { getRounds } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";

export const dynamic = "force-dynamic";

export default async function RoundsPage() {
  const rounds = await getRounds(0, 20);

  return (
    <section className="panel">
      <p className="eyebrow">회차 목록</p>
      <h1 className="page-title">최근 회차 20건</h1>
      <div className="round-list">
        {rounds.items.map((item) => (
          <article key={item.round} className="round-card">
            <div className="round-meta">
              <div>
                <strong>{item.round}회</strong>
                <p className="muted">{formatDrawDate(item.drawDate)}</p>
              </div>
              <Link href={`/rounds/${item.round}`} className="button secondary">
                상세 보기
              </Link>
            </div>
            <LottoBalls numbers={item.numbers} bonusNumber={item.bonusNumber} />
            <p className="muted">1등 당첨금 {formatCurrency(item.firstPrizeAmount)}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
