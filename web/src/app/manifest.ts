import type { MetadataRoute } from "next";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "KRAFT Lotto",
    short_name: "KRAFT",
    description: "KST 기준 최신 로또 회차, 번호 추천, 저장함",
    start_url: "/",
    display: "standalone",
    background_color: "#f6f1e8",
    theme_color: "#c94f24",
    icons: [
      {
        src: "/icon",
        sizes: "32x32",
        type: "image/png",
      },
      {
        src: "/apple-icon",
        sizes: "180x180",
        type: "image/png",
        purpose: "maskable",
      },
    ],
  };
}
