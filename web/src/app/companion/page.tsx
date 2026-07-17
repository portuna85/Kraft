import type { Metadata } from "next";
import { CompanionFilterClient } from "@/components/companion-filter-client";
import { getCompanionStats } from "@/lib/api";
import logger from "@/lib/logger";

// 루트 레이아웃이 CSP nonce를 위해 headers()를 호출해 전 페이지가 동적 렌더링되므로
// 이 값은 Full Route Cache에 영향을 주지 않는다(문서화 목적으로 유지, 실제 캐시는 lib/api.ts의 fetch revalidate가 담당).
export const revalidate = 1800;

export const metadata: Metadata = {
  title: "동반 출현",
  description: "로또 6/45에서 함께 자주 나온 번호 조합을 분석해 동반 출현 통계를 제공합니다.",
  alternates: { canonical: "/companion" },
};

export default async function CompanionPage() {
  // 이 페이지의 유일한 핵심 데이터 — 실패를 200 폴백으로 숨기지 않고 error.tsx(5xx)로 넘긴다.
  let stats;
  try {
    stats = await getCompanionStats();
  } catch (error) {
    logger.error({ err: error }, "동반 출현 통계 조회 실패 — 핵심 데이터 실패로 페이지 오류 처리");
    throw error;
  }

  // 초기 payload는 상위 50개만 전달해 SSR/RSC 응답 크기를 줄인다. 번호 필터 선택 시
  // 클라이언트가 서버의 번호별 필터 API(ball 파라미터)를 호출해 상위 50개 밖의 번호도 정확히 매칭한다.
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
