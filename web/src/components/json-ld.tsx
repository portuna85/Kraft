type JsonLdWebSiteProps = {
  baseUrl: string;
  nonce?: string;
};

export function JsonLdWebSite({ baseUrl, nonce }: JsonLdWebSiteProps) {
  const schema = {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebSite",
        "@id": `${baseUrl}/#website`,
        url: baseUrl,
        name: "KRAFT Lotto",
        description: "로또 당첨 결과 조회, 추천 조합 생성, 저장 번호 관리를 제공하는 서비스입니다.",
        inLanguage: "ko-KR",
      },
      {
        "@type": "Organization",
        "@id": `${baseUrl}/#organization`,
        url: baseUrl,
        name: "KRAFT Lotto",
      },
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

type JsonLdLottoRoundProps = {
  baseUrl: string;
  round: number;
  drawDate: string;
  nonce?: string;
};

export function JsonLdLottoRound({ baseUrl, round, drawDate, nonce }: JsonLdLottoRoundProps) {
  const schema = {
    "@context": "https://schema.org",
    "@type": "WebPage",
    "@id": `${baseUrl}/rounds`,
    url: `${baseUrl}/rounds`,
    name: `제${round}회 로또 당첨번호 (${drawDate})`,
    description: `제${round}회 로또 당첨 번호, 보너스 번호, 추첨일과 회차 정보를 확인할 수 있습니다.`,
    inLanguage: "ko-KR",
    isPartOf: { "@id": `${baseUrl}/#website` },
    breadcrumb: {
      "@type": "BreadcrumbList",
      itemListElement: [
        { "@type": "ListItem", position: 1, name: "홈", item: baseUrl },
        { "@type": "ListItem", position: 2, name: "결과 · 회차", item: `${baseUrl}/rounds` },
      ],
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
