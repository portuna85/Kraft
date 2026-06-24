// Edge runtime용 폰트 로더 (fs/path 사용 불가 → jsDelivr CDN fetch)
// Node.js runtime(opengraph-image)은 og-font-node.ts 사용.

type FontEntry = {
  name: string;
  data: ArrayBuffer;
  weight: 700;
  style: "normal";
};

export type FontConfig = {
  fonts: FontEntry[];
  fontFamily: string;
};

let edgeCache: FontEntry[] | null = null;

const CDN = "https://cdn.jsdelivr.net/npm/@fontsource/noto-sans-kr@5/files";

export async function getOgFontConfig(): Promise<FontConfig> {
  if (edgeCache) return { fonts: edgeCache, fontFamily: "Noto Sans KR, sans-serif" };
  try {
    const [korean, latin] = await Promise.all([
      fetch(`${CDN}/noto-sans-kr-korean-700-normal.woff`).then((r) => r.arrayBuffer()),
      fetch(`${CDN}/noto-sans-kr-latin-700-normal.woff`).then((r) => r.arrayBuffer()),
    ]);
    edgeCache = [
      { name: "Noto Sans KR", data: korean, weight: 700, style: "normal" },
      { name: "Noto Sans KR", data: latin, weight: 700, style: "normal" },
    ];
    return { fonts: edgeCache, fontFamily: "Noto Sans KR, sans-serif" };
  } catch {
    return { fonts: [], fontFamily: "sans-serif" };
  }
}
