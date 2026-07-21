import type { Metadata } from "next";
import { headers } from "next/headers";
import { FrequencyFilterClient } from "@/components/frequency-filter-client";
import { AdSenseSidebar, AdSenseUnit, PageAd } from "@/components/ad-unit";
import { JsonLdBreadcrumb } from "@/components/json-ld";
import { getFrequencyStats, getPublicBaseUrl } from "@/lib/api";
import logger from "@/lib/logger";

export const metadata: Metadata = {
  title: "출현 통계",
  description: "로또 6/45 모든 회차를 기준으로 번호별 출현 횟수와 비율을 확인할 수 있습니다.",
  alternates: { canonical: "/frequency" },
};

export default async function FrequencyPage() {
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const baseUrl = getPublicBaseUrl();

  // 이 페이지의 유일한 핵심 데이터 — 실패를 200 폴백으로 숨기지 않고 error.tsx(5xx)로 넘긴다.
  let stats;
  try {
    stats = await getFrequencyStats();
  } catch (error) {
    logger.error({ err: error }, "출현 통계 조회 실패 — 핵심 데이터 실패로 페이지 오류 처리");
    throw error;
  }

  return (
    <div className="page-with-sidebar">
      <section className="panel">
        <JsonLdBreadcrumb baseUrl={baseUrl} nonce={nonce} items={[{ name: "출현 통계", item: `${baseUrl}/frequency` }]} />
        <p className="eyebrow">출현 통계</p>
        <h1 className="page-title">번호 출현 통계</h1>
        <FrequencyFilterClient initial={stats} />
        <PageAd slot="frequency" />
        <AdSenseUnit
          slot={process.env.NEXT_PUBLIC_ADSENSE_UNIT_FREQUENCY_MOBILE ?? ""}
          width={300}
          height={250}
          className="ad-mobile"
        />
        <AdSenseUnit
          slot={process.env.NEXT_PUBLIC_ADSENSE_UNIT_FREQUENCY ?? ""}
          width={728}
          height={90}
          className="ad-desktop"
        />
      </section>
      <AdSenseSidebar slot={process.env.NEXT_PUBLIC_ADSENSE_UNIT_SIDEBAR ?? ""} />
    </div>
  );
}
