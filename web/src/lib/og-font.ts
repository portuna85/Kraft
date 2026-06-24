// Edge runtime과 Node.js runtime 모두에서 동작
// 각 런타임 인스턴스마다 최초 1회 fetch, 이후 메모리 캐시

let cachedFont: ArrayBuffer | null = null;

export async function loadKoreanFont(): Promise<ArrayBuffer | null> {
  if (cachedFont) return cachedFont;
  try {
    const css = await fetch(
      "https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@700&display=swap",
      {
        headers: {
          "User-Agent":
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        },
      },
    ).then((r) => r.text());

    const match = css.match(/src: url\(([^)]+)\) format\('woff2'\)/);
    if (!match) return null;

    cachedFont = await fetch(match[1]).then((r) => r.arrayBuffer());
    return cachedFont;
  } catch {
    return null;
  }
}

type FontConfig = {
  fonts: { name: string; data: ArrayBuffer; weight: 700; style: "normal" }[];
  fontFamily: string;
};

export async function getOgFontConfig(): Promise<FontConfig> {
  const data = await loadKoreanFont();
  if (!data) return { fonts: [], fontFamily: "sans-serif" };
  return {
    fonts: [{ name: "Noto Sans KR", data, weight: 700, style: "normal" }],
    fontFamily: "Noto Sans KR, sans-serif",
  };
}
