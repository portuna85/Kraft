import type { Metadata } from "next";
import { CompanionFilterClient } from "@/components/companion-filter-client";
import { getCompanionStats } from "@/lib/api";
export const revalidate = 1800;

export const metadata: Metadata = {
  title: "동반 출현 | KRAFT Lotto",
  description: "로또 6/45에서 두 번호가 같은 회차에 함께 출현한 빈도를 분석한 동반 출현 통계입니다.",
  alternates: { canonical: "/companion" },
};

export default async function CompanionPage() {
  const stats = await getCompanionStats();
  const pairs = stats.topPairs.slice(0, 50);

  return (
    <section className="panel">
      <p className="eyebrow">동반 출현</p>
      <h1 className="page-title">동반 출현 번호</h1>
      <p className="muted" style={{ marginTop: 0 }}>총 {stats.totalRounds}회 기준 상위 {pairs.length}개</p>
      <CompanionFilterClient pairs={pairs} totalRounds={stats.totalRounds} />
    </section>
  );
}
