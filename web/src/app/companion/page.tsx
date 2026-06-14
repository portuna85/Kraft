import type { Metadata } from "next";
import { getCompanionStats } from "@/lib/api";
import { CompanionFilterClient } from "@/components/companion-filter-client";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "동반 출현 통계 | KRAFT Lotto",
  description: "같은 회차에서 함께 나온 번호 조합을 기준으로 동반 출현 빈도를 확인합니다.",
  alternates: { canonical: "/companion" }
};

export default async function CompanionPage() {
  const stats = await getCompanionStats();
  const pairs = stats.topPairs.slice(0, 50);

  return (
    <section className="panel">
      <p className="eyebrow">통계</p>
      <h1 className="page-title">동반 출현 상위 조합</h1>
      <p className="page-subtitle">
        총 {stats.totalRounds}회 기준으로 같은 회차에 자주 함께 등장한 번호 쌍 {pairs.length}개를 보여줍니다.
        번호를 선택하면 해당 번호가 포함된 쌍만 필터합니다.
      </p>
      <CompanionFilterClient pairs={pairs} totalRounds={stats.totalRounds} />
    </section>
  );
}
