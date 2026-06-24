// 루트 레이아웃이 모든 요청에서 inline script CSP nonce를 위해 headers()를 호출하면
// force-static/ISR 페이지까지 강제로 동적 렌더링된다. 이 두 스크립트는 배포 수명 동안
// 내용이 고정이므로 nonce 대신 정적 sha256 해시로 허용한다(web/src/proxy.ts에서 사용).
export const THEME_INIT_SCRIPT =
  "try{if(localStorage.getItem('kraft-theme')==='dark'){document.documentElement.setAttribute('data-theme','dark');}}catch(e){}";

export const FAQ_ITEMS: { question: string; answer: string }[] = [
  {
    question: "추천 번호로 사면 당첨 확률이 높아지나요?",
    answer:
      "아닙니다. KRAFT Lotto의 추천 번호는 완전 무작위 방식으로 생성되며, 특정 조합이 다른 조합보다 더 유리해지도록 설계되어 있지 않습니다. 로또 6/45의 1등 확률은 모든 조합이 동일합니다.",
  },
  {
    question: "저장함은 어디에 저장되나요?",
    answer:
      "저장 번호는 브라우저에 자동 생성되는 익명 기기 토큰과 연결되어 서버에 저장됩니다. 브라우저 데이터를 삭제하거나 다른 기기에서 접속하면 같은 목록을 바로 이어서 볼 수 없습니다.",
  },
  {
    question: "당첨 번호가 얼마나 빨리 업데이트되나요?",
    answer:
      "추첨 결과 공개 후 자동 수집을 통해 최신 회차를 반영합니다. 일반적으로 수 분 내 반영되지만, 외부 데이터 상황에 따라 다소 지연될 수 있습니다.",
  },
  {
    question: "몇 회차부터 데이터가 있나요?",
    answer: "2002년 12월 7일 추첨한 제1회부터 최신 회차까지의 전체 이력을 제공합니다.",
  },
  {
    question: "앱이 있나요?",
    answer: "현재는 웹 서비스 중심으로 제공하고 있으며, 모바일 브라우저에서도 동일하게 사용할 수 있습니다.",
  },
  {
    question: "오류나 개선 요청은 어떻게 하나요?",
    answer: "문의하기 페이지를 통해 오류 제보와 개선 요청을 보내 주세요.",
  },
];

export function buildFaqPageJsonLd() {
  return {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    mainEntity: FAQ_ITEMS.map((item) => ({
      "@type": "Question",
      name: item.question,
      acceptedAnswer: { "@type": "Answer", text: item.answer },
    })),
  };
}

export function buildWebsiteJsonLd(baseUrl: string) {
  return {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebSite",
        "@id": `${baseUrl}/#website`,
        url: baseUrl,
        name: "KRAFT Lotto",
        description: "로또 당첨 결과 조회, 추천 조합 생성, 저장 번호 관리를 제공하는 서비스입니다.",
        inLanguage: "ko-KR",
        potentialAction: {
          "@type": "SearchAction",
          target: {
            "@type": "EntryPoint",
            urlTemplate: `${baseUrl}/rounds/{search_term_string}`,
          },
          "query-input": "required name=search_term_string",
        },
      },
      {
        "@type": "Organization",
        "@id": `${baseUrl}/#organization`,
        url: baseUrl,
        name: "KRAFT Lotto",
      },
    ],
  };
}
