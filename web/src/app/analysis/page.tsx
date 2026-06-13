import type { Metadata } from "next";
import { AnalysisClient } from "@/components/analysis-client";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "번호 조합 분석",
  description: "입력한 번호 조합의 통계적 특성을 분석합니다.",
  alternates: { canonical: "/analysis" }
};

export default function AnalysisPage() {
  return (
    <section className="panel">
      <p className="eyebrow">분석</p>
      <h1 className="page-title">번호 조합 분석</h1>
      <p className="page-subtitle">번호 6개를 입력하면 홀짝 분포, 합계 구간, 연속 번호 등을 분석합니다.</p>
      <AnalysisClient />
    </section>
  );
}
