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
        description: "KST 기준 최신 로또 회차, 번호 추천, 저장함을 제공합니다.",
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
    "@id": `${baseUrl}/latest`,
    url: `${baseUrl}/latest`,
    name: `제${round}회 로또 당첨번호 (${drawDate})`,
    description: `제${round}회 로또 당첨 번호와 보너스 번호, 추첨일을 KST 기준으로 확인합니다.`,
    inLanguage: "ko-KR",
    isPartOf: { "@id": `${baseUrl}/#website` },
    breadcrumb: {
      "@type": "BreadcrumbList",
      itemListElement: [
        { "@type": "ListItem", position: 1, name: "홈", item: baseUrl },
        { "@type": "ListItem", position: 2, name: "최신 회차", item: `${baseUrl}/latest` },
      ],
    },
  };

  return (
    <script
      type="application/ld+json"
      nonce={nonce}
      dangerouslySetInnerHTML={{ __html: JSON.stringify(schema) }}
    />
  );
}
