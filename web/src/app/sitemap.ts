import type { MetadataRoute } from "next";
import { getLatestWinningNumber, getPublicBaseUrl } from "@/lib/api";

// 레이아웃 밖 라우트 핸들러라 페이지의 revalidate와 달리 실제로 Full Route Cache에 적용된다.
// Next.js 세그먼트 설정 export는 리터럴이어야 정적 분석이 되므로 lib/revalidate.ts의
// REVALIDATE_SITEMAP(=3600)과 값을 수동으로 맞춘다(import 시 빌드 실패).
export const revalidate = 3600;

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = getPublicBaseUrl();
  let latestRound = 0;
  let lastMod: string | undefined;
  try {
    const latest = await getLatestWinningNumber();
    latestRound = latest.round;
    lastMod = `${latest.drawDate}T00:00:00+09:00`;
  } catch {
    // backend unavailable (e.g. during build); omit lastModified and round entries
  }

  // blueprint §14.1: 실존 URL만, 리다이렉트 없는 최종 형태로 등재
  const staticRoutes: MetadataRoute.Sitemap = [
    { url: `${baseUrl}/`,          lastModified: lastMod, changeFrequency: "weekly",  priority: 1.0 },
    { url: `${baseUrl}/rounds`,    lastModified: lastMod, changeFrequency: "weekly",  priority: 0.9 },
    { url: `${baseUrl}/frequency`, lastModified: lastMod, changeFrequency: "weekly",  priority: 0.8 },
    { url: `${baseUrl}/recommend`, lastModified: lastMod, changeFrequency: "weekly",  priority: 0.8 },
    { url: `${baseUrl}/stats`,     lastModified: lastMod, changeFrequency: "weekly",  priority: 0.7 },
    { url: `${baseUrl}/analysis`,  lastModified: lastMod, changeFrequency: "weekly",  priority: 0.7 },
    { url: `${baseUrl}/companion`, lastModified: lastMod, changeFrequency: "weekly",  priority: 0.7 },

    { url: `${baseUrl}/info/faq`,              lastModified: "2026-01-01", changeFrequency: "monthly", priority: 0.6 },
    { url: `${baseUrl}/info/data-source`,      lastModified: "2026-01-01", changeFrequency: "monthly", priority: 0.5 },
    { url: `${baseUrl}/info/methodology`,      lastModified: "2026-01-01", changeFrequency: "monthly", priority: 0.5 },
    { url: `${baseUrl}/info/responsible-play`, lastModified: "2026-01-01", changeFrequency: "monthly", priority: 0.5 },
    { url: `${baseUrl}/info/contact`,          lastModified: "2026-01-01", changeFrequency: "monthly", priority: 0.5 },
    { url: `${baseUrl}/info/privacy`,          lastModified: "2026-06-24", changeFrequency: "yearly",  priority: 0.4 },
    { url: `${baseUrl}/info/terms`,            lastModified: "2026-01-01", changeFrequency: "yearly",  priority: 0.4 },
  ];

  const roundRoutes: MetadataRoute.Sitemap = latestRound > 0
    ? Array.from({ length: latestRound }, (_, i) => ({
        url: `${baseUrl}/rounds/${i + 1}`,
        changeFrequency: "yearly" as const,
        priority: i + 1 === latestRound ? 0.9 : 0.5,
        ...(i + 1 === latestRound ? { lastModified: lastMod } : {}),
      }))
    : [];

  return [...staticRoutes, ...roundRoutes];
}
