import type { Metadata } from "next";
import { CompanionFilterClient } from "@/components/companion-filter-client";
import { getCompanionStats } from "@/lib/api";
import logger from "@/lib/logger";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "동반 출현",
  description: "로또 6/45에서 함께 자주 나온 번호 조합을 분석해 동반 출현 통계를 제공합니다.",
  alternates: { canonical: "/companion" },
};

export default async function CompanionPage() {
  const stats = await getCompanionStats().catch((error) => {
    logger.warn({ err: error }, "동반 출현 통계 조회 실패");
    return null;
  });

  if (!stats) {
    return (
      <section className="panel">
        <p className="eyebrow">동반 출현</p>
        <h1 className="page-title">통계를 준비 중입니다</h1>
        <p className="page-subtitle">잠시 후 다시 확인해 주세요.</p>
      </section>
    );
  }

  // 초기 payload는 상위 50개만 전달해 SSR/RSC 응답 크기를 줄인다. 번호 필터 선택 시
  // 클라이언트가 전체 990쌍을 지연 fetch해 상위 50개 밖의 번호도 정확히 매칭한다.
  const initialPairs = stats.topPairs.slice(0, 50);

  return (
    <section className="panel">
      <p className="eyebrow">동반 출현</p>
      <h1 className="page-title">동반 출현 번호</h1>
      <p className="muted panel-lead">총 {stats.totalRounds}회 기준 전체 {stats.topPairs.length}개 조합 (기본 상위 50개 표시)</p>
      <CompanionFilterClient pairs={initialPairs} totalRounds={stats.totalRounds} />
    </section>
  );
}
