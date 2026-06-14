import type { Metadata } from "next";
import { FrequencyFilterClient } from "@/components/frequency-filter-client";
import { getFrequencyStats } from "@/lib/api";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "출현 통계 | KRAFT Lotto",
  description: "자주 나온 번호와 덜 나온 번호를 확인할 수 있습니다.",
  alternates: { canonical: "/frequency" },
};

export default async function FrequencyPage() {
  const stats = await getFrequencyStats();

  return (
    <section className="panel">
      <p className="eyebrow">출현 통계</p>
      <h1 className="page-title">번호별 출현 통계</h1>
      <p className="page-subtitle">자주 나온 번호와 분포를 빠르게 비교할 수 있습니다.</p>
      <FrequencyFilterClient initial={stats} />
    </section>
  );
}
