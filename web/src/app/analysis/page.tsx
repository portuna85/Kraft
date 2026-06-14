import type { Metadata } from "next";
import { AnalysisClient } from "@/components/analysis-client";

export const metadata: Metadata = {
  title: "번호 조합 분석",
  description: "입력한 6개 번호의 분포와 기본 통계를 빠르게 확인할 수 있습니다.",
  alternates: { canonical: "/analysis" },
};

export default function AnalysisPage() {
  return (
    <section className="panel">
      <p className="eyebrow">번호 분석</p>
      <h1 className="page-title">번호 조합을 바로 분석해 보세요</h1>
      <p className="page-subtitle">번호 6개의 분포와 합계를 한눈에 확인할 수 있습니다.</p>
      <AnalysisClient />
    </section>
  );
}
