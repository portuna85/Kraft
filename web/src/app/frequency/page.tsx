import type { Metadata } from "next";
import { getFrequencyStats } from "@/lib/api";
import { FrequencyFilterClient } from "@/components/frequency-filter-client";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "번호 빈도 통계 | KRAFT Lotto",
  description: "번호별 출현 빈도를 기준으로 자주 나온 번호와 드물게 나온 번호를 확인합니다.",
  alternates: { canonical: "/frequency" }
};

export default async function FrequencyPage() {
  const stats = await getFrequencyStats();

  return (
    <section className="panel">
      <p className="eyebrow">통계</p>
      <h1 className="page-title">번호별 출현 빈도</h1>
      <FrequencyFilterClient initial={stats} />
    </section>
  );
}
