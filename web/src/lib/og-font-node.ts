// Node.js runtime 전용 폰트 로더 (opengraph-image.tsx에서 사용).
// Edge runtime route에서는 절대 import 금지 → og-font.ts 사용.

import { readFile } from "fs/promises";
import { join } from "path";

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

let nodeCache: FontEntry[] | null = null;

export async function getOgFontConfig(): Promise<FontConfig> {
  if (nodeCache) return { fonts: nodeCache, fontFamily: "Noto Sans KR, sans-serif" };
  try {
    const base = join(process.cwd(), "public", "fonts");
    const [korean, latin] = await Promise.all([
      readFile(join(base, "NotoSansKR-Bold-korean.woff")),
      readFile(join(base, "NotoSansKR-Bold-latin.woff")),
    ]);
    nodeCache = [
      { name: "Noto Sans KR", data: korean.buffer as ArrayBuffer, weight: 700, style: "normal" },
      { name: "Noto Sans KR", data: latin.buffer as ArrayBuffer, weight: 700, style: "normal" },
    ];
    return { fonts: nodeCache, fontFamily: "Noto Sans KR, sans-serif" };
  } catch {
    return { fonts: [], fontFamily: "sans-serif" };
  }
}
