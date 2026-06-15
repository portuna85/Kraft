import type { MetadataRoute } from "next";
import { getLatestWinningNumber, getPublicBaseUrl } from "@/lib/api";
import { REVALIDATE_SITEMAP } from "@/lib/revalidate";

export const revalidate = REVALIDATE_SITEMAP;

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
    { url: `${baseUrl}/`, lastModified: lastMod },
    { url: `${baseUrl}/latest`, lastModified: lastMod },
    { url: `${baseUrl}/rounds`, lastModified: lastMod },
    { url: `${baseUrl}/frequency`, lastModified: lastMod },
    { url: `${baseUrl}/stats`, lastModified: lastMod },
    { url: `${baseUrl}/analysis`, lastModified: lastMod },
    { url: `${baseUrl}/companion`, lastModified: lastMod },
    { url: `${baseUrl}/recommend`, lastModified: lastMod },

    { url: `${baseUrl}/info/data-source`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/methodology`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/faq`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/privacy`, changeFrequency: "yearly" },
    { url: `${baseUrl}/info/terms`, changeFrequency: "yearly" },
    { url: `${baseUrl}/info/contact`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/responsible-play`, changeFrequency: "monthly" },
  ];

  const roundRoutes: MetadataRoute.Sitemap = latestRound > 0
    ? Array.from({ length: latestRound }, (_, i) => ({
        url: `${baseUrl}/rounds/${i + 1}`,
        changeFrequency: "yearly" as const,
        ...(i + 1 === latestRound ? { lastModified: lastMod } : {}),
      }))
    : [];

  return [...staticRoutes, ...roundRoutes];
}
