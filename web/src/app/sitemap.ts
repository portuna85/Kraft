import type { MetadataRoute } from "next";
import { getLatestWinningNumber, getPublicBaseUrl } from "@/lib/api";

export const revalidate = 300;

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = getPublicBaseUrl();
  const latest = await getLatestWinningNumber();
  const lastMod = `${latest.drawDate}T00:00:00+09:00`;

  // blueprint §14.1: 실존 URL만, 리다이렉트 없는 최종 형태로 등재
  const staticRoutes: MetadataRoute.Sitemap = [
    { url: `${baseUrl}/`, lastModified: lastMod },
    { url: `${baseUrl}/latest`, lastModified: lastMod },
    { url: `${baseUrl}/rounds`, lastModified: lastMod },
    { url: `${baseUrl}/frequency`, lastModified: lastMod },
    { url: `${baseUrl}/stats`, lastModified: lastMod },
    { url: `${baseUrl}/analysis`, lastModified: lastMod },
    { url: `${baseUrl}/companion`, lastModified: lastMod },
    { url: `${baseUrl}/info/data-source`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/methodology`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/faq`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/privacy`, changeFrequency: "yearly" },
    { url: `${baseUrl}/info/terms`, changeFrequency: "yearly" },
    { url: `${baseUrl}/info/contact`, changeFrequency: "monthly" },
    { url: `${baseUrl}/info/responsible-play`, changeFrequency: "monthly" },
  ];

  return staticRoutes;
}
