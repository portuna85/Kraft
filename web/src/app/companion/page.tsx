import type { Metadata } from "next";
import { CompanionFilterClient } from "@/components/companion-filter-client";
import { getCompanionStats } from "@/lib/api";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "동반 출현 | KRAFT Lotto",
  description: "로또 6/45에서 함께 자주 나온 번호 조합을 분석해 동반 출현 통계를 제공합니다.",
  alternates: { canonical: "/companion" },
};

export default async function CompanionPage() {
  const stats = await getCompanionStats();
  const pairs = stats.topPairs.slice(0, 50);

  return (
    <section className="panel">
      <p className="eyebrow">동반 출현</p>
      <h1 className="page-title">동반 출현 번호</h1>
      <p className="muted panel-lead">총 {stats.totalRounds}회 기준 상위 {pairs.length}개</p>
      <CompanionFilterClient pairs={pairs} totalRounds={stats.totalRounds} />
    </section>
  );
}
