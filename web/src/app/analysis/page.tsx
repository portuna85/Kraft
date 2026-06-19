import type { Metadata } from "next";
import { AnalysisClient } from "@/components/analysis-client";

export const metadata: Metadata = {
  title: "번호 조합 분석",
  description: "번호 6개를 입력하면 홀짝, 합계, 구간 분포 등 기본 통계를 분석합니다.",
  alternates: { canonical: "/analysis" },
};

export default function AnalysisPage() {
  return (
    <section className="panel">
      <p className="eyebrow">번호 분석</p>
      <h1 className="page-title">번호 조합 분석</h1>
      <AnalysisClient />
    </section>
  );
}
