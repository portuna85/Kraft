import type { Metadata } from "next";
import { FrequencyFilterClient } from "@/components/frequency-filter-client";
import { getFrequencyStats } from "@/lib/api";
import logger from "@/lib/logger";

export const revalidate = 1800;

export const metadata: Metadata = {
  title: "출현 통계",
  description: "로또 6/45 모든 회차를 기준으로 번호별 출현 횟수와 비율을 확인할 수 있습니다.",
  alternates: { canonical: "/frequency" },
};

export default async function FrequencyPage() {
  const stats = await getFrequencyStats().catch((error) => {
    logger.warn({ err: error }, "출현 통계 조회 실패");
    return null;
  });

  if (!stats) {
    return (
      <section className="panel">
        <p className="eyebrow">출현 통계</p>
        <h1 className="page-title">통계를 준비 중입니다</h1>
        <p className="page-subtitle">잠시 후 다시 확인해 주세요.</p>
      </section>
    );
  }

  return (
    <section className="panel">
      <p className="eyebrow">출현 통계</p>
      <h1 className="page-title">번호 출현 통계</h1>
      <FrequencyFilterClient initial={stats} />
    </section>
  );
}
