import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  // blueprint §6.2: 비-슬래시 단일화
  trailingSlash: false,
  async redirects() {
    return [
      // blueprint §6.2: /data-source → /info/data-source 301
      {
        source: "/data-source",
        destination: "/info/data-source",
        permanent: true
      }
    ];
  },
  async rewrites() {
    const backendUrl =
      process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";
    return [
      {
        source: "/ops-api/:path*",
        destination: `${backendUrl}/ops/:path*`
      }
    ];
  }
};

export default nextConfig;
