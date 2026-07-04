import type { Metadata } from "next";
import { headers } from "next/headers";
import { FrequencyFilterClient } from "@/components/frequency-filter-client";
import { PageAd } from "@/components/ad-unit";
import { JsonLdBreadcrumb } from "@/components/json-ld";
import { getFrequencyStats, getPublicBaseUrl } from "@/lib/api";
import logger from "@/lib/logger";

// 루트 레이아웃이 CSP nonce를 위해 headers()를 호출해 전 페이지가 동적 렌더링되므로
// 이 값은 Full Route Cache에 영향을 주지 않는다(문서화 목적으로 유지, 실제 캐시는 lib/api.ts의 fetch revalidate가 담당).
export const revalidate = 1800;

export const metadata: Metadata = {
  title: "출현 통계",
  description: "로또 6/45 모든 회차를 기준으로 번호별 출현 횟수와 비율을 확인할 수 있습니다.",
  alternates: { canonical: "/frequency" },
};

export default async function FrequencyPage() {
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const baseUrl = getPublicBaseUrl();
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
      <JsonLdBreadcrumb baseUrl={baseUrl} nonce={nonce} items={[{ name: "출현 통계", item: `${baseUrl}/frequency` }]} />
      <p className="eyebrow">출현 통계</p>
      <h1 className="page-title">번호 출현 통계</h1>
      <FrequencyFilterClient initial={stats} />
      <PageAd slot="frequency" />
    </section>
  );
}
