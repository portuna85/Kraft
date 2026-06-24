// Edge runtime과 Node.js runtime 모두에서 동작하는 폰트 로더.
//
// Node.js 환경 (opengraph-image.tsx): 디스크(public/fonts/)에서 직접 읽음.
// Edge 환경 (api/og/round/route.tsx): jsDelivr CDN에서 직접 woff2 binary fetch.
// 두 경우 모두 모듈 스코프 캐시로 재요청 시 재다운로드 방지.

type FontEntry = {
  name: string;
  data: ArrayBuffer;
  weight: 700;
  style: "normal";
};

type FontConfig = {
  fonts: FontEntry[];
  fontFamily: string;
};

// ── Node.js 전용 (디스크 읽기) ────────────────────────────────────────────

let nodeCache: FontEntry[] | null = null;

async function loadFontsNode(): Promise<FontEntry[]> {
  if (nodeCache) return nodeCache;
  const { readFile } = await import("fs/promises");
  const { join } = await import("path");
  const base = join(process.cwd(), "public", "fonts");
  const [korean, latin] = await Promise.all([
    readFile(join(base, "NotoSansKR-Bold-korean.woff2")),
    readFile(join(base, "NotoSansKR-Bold-latin.woff2")),
  ]);
  nodeCache = [
    { name: "Noto Sans KR", data: korean.buffer as ArrayBuffer, weight: 700, style: "normal" },
    { name: "Noto Sans KR", data: latin.buffer as ArrayBuffer, weight: 700, style: "normal" },
  ];
  return nodeCache;
}

// ── Edge 전용 (CDN fetch) ─────────────────────────────────────────────────

let edgeCache: FontEntry[] | null = null;

const CDN = "https://cdn.jsdelivr.net/npm/@fontsource/noto-sans-kr@5/files";

async function loadFontsEdge(): Promise<FontEntry[]> {
  if (edgeCache) return edgeCache;
  const [korean, latin] = await Promise.all([
    fetch(`${CDN}/noto-sans-kr-korean-700-normal.woff2`).then((r) => r.arrayBuffer()),
    fetch(`${CDN}/noto-sans-kr-latin-700-normal.woff2`).then((r) => r.arrayBuffer()),
  ]);
  edgeCache = [
    { name: "Noto Sans KR", data: korean, weight: 700, style: "normal" },
    { name: "Noto Sans KR", data: latin, weight: 700, style: "normal" },
  ];
  return edgeCache;
}

// ── 공용 ─────────────────────────────────────────────────────────────────

export async function getOgFontConfig(): Promise<FontConfig> {
  try {
    const isEdge = process.env.NEXT_RUNTIME === "edge";
    const fonts = isEdge ? await loadFontsEdge() : await loadFontsNode();
    return { fonts, fontFamily: "Noto Sans KR, sans-serif" };
  } catch {
    return { fonts: [], fontFamily: "sans-serif" };
  }
}
