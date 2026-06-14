import type { Metadata } from "next";
import { AnalysisClient } from "@/components/analysis-client";

export const metadata: Metadata = {
  title: "번호 조합 분석",
  description: "입력한 6개 번호 조합의 분포와 기본 통계 특성을 빠르게 확인합니다.",
  alternates: { canonical: "/analysis" }
};

export default function AnalysisPage() {
  return (
    <section className="panel">
      <p className="eyebrow">분석</p>
      <h1 className="page-title">내 번호 조합 분석</h1>
      <p className="page-subtitle">선택한 번호 6개의 균형, 합계, 연속 번호 여부를 바로 점검해 보세요.</p>
      <AnalysisClient />
    </section>
  );
}
