import { buildWebsiteJsonLd } from "@/lib/csp-inline-scripts";
import type { AnalysisResponse } from "@/lib/api";

type BreadcrumbItem = { name: string; item?: string };

type JsonLdBreadcrumbProps = {
  baseUrl: string;
  nonce?: string;
  items: BreadcrumbItem[];
};

export function JsonLdBreadcrumb({ baseUrl, nonce, items }: JsonLdBreadcrumbProps) {
  const schema = {
    "@context": "https://schema.org",
    "@type": "BreadcrumbList",
    itemListElement: [
      { "@type": "ListItem", position: 1, name: "홈", item: baseUrl },
      ...items.map((crumb, index) => ({
        "@type": "ListItem",
        position: index + 2,
        name: crumb.name,
        ...(crumb.item ? { item: crumb.item } : {}),
      })),
    ],
  };
  return (
    <script
      type="application/ld+json"
      nonce={nonce}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
    />
  );
}

type JsonLdWebSiteProps = {
  baseUrl: string;
  nonce?: string;
};

export function JsonLdWebSite({ baseUrl, nonce }: JsonLdWebSiteProps) {
  return (
    <script
      type="application/ld+json"
      nonce={nonce}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: JSON.stringify(buildWebsiteJsonLd(baseUrl)) }}
    />
  );
}

type JsonLdLottoRoundProps = {
  baseUrl: string;
  round: number;
  drawDate: string;
  nonce?: string;
  /** 이 스키마가 설명하는 실제 페이지 URL. 기본값은 회차 상세 페이지(/rounds/{round}). */
  pageUrl?: string;
  analysis?: AnalysisResponse | null;
};

export function JsonLdLottoRound({ baseUrl, round, drawDate, nonce, pageUrl, analysis }: JsonLdLottoRoundProps) {
  const url = pageUrl ?? `${baseUrl}/rounds/${round}`;
  const breadcrumbItems = [
    { "@type": "ListItem", position: 1, name: "홈", item: baseUrl },
    { "@type": "ListItem", position: 2, name: "결과 · 회차", item: `${baseUrl}/rounds` },
  ];
  if (url !== `${baseUrl}/rounds`) {
    breadcrumbItems.push({ "@type": "ListItem", position: 3, name: `제${round}회`, item: url });
  }
  const baseDescription = `제${round}회 로또 당첨 번호, 보너스 번호, 추첨일과 회차 정보를 확인할 수 있습니다.`;
  const description = analysis
    ? `${baseDescription} 홀수 ${analysis.oddCount}개·짝수 ${analysis.evenCount}개, 번호 합계 ${analysis.sumOfNumbers}(${analysis.sumBucket} 구간).`
    : baseDescription;
  const schema = {
    "@context": "https://schema.org",
    "@type": "WebPage",
    "@id": url,
    url,
    name: `제${round}회 로또 당첨번호 (${drawDate})`,
    description,
    inLanguage: "ko-KR",
    isPartOf: { "@id": `${baseUrl}/#website` },
    breadcrumb: {
      "@type": "BreadcrumbList",
      itemListElement: breadcrumbItems,
    },
  };

  return (
    <script
      type="application/ld+json"
      nonce={nonce}
      suppressHydrationWarning
      dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
    />
  );
}
