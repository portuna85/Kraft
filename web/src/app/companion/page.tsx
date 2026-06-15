import type { Metadata } from "next";
import { CompanionFilterClient } from "@/components/companion-filter-client";
import { getCompanionStats } from "@/lib/api";
import { REVALIDATE_STATS } from "@/lib/revalidate";

export const revalidate = REVALIDATE_STATS;

export const metadata: Metadata = {
  title: "동반 출현 | KRAFT Lotto",
  description: "함께 자주 나온 번호 조합을 확인할 수 있습니다.",
  alternates: { canonical: "/companion" },
};

export default async function CompanionPage() {
  const stats = await getCompanionStats();
  const pairs = stats.topPairs.slice(0, 50);

  return (
    <section className="panel">
      <p className="eyebrow">동반 출현</p>
      <h1 className="page-title">함께 나온 번호 조합</h1>
      <p className="page-subtitle">총 {stats.totalRounds}회 기준 상위 {pairs.length}개 조합을 보여줍니다.</p>
      <CompanionFilterClient pairs={pairs} totalRounds={stats.totalRounds} />
    </section>
  );
}
