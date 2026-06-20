import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  poweredByHeader: false,
  allowedDevOrigins: ["127.0.0.1"],
  async redirects() {
    return [
      {
        source: "/data-source",
        destination: "/info/data-source",
        permanent: true,
      },
      {
        source: "/latest",
        destination: "/rounds",
        permanent: true,
      },
    ];
  },
  async rewrites() {
    const backendUrl = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

    return [
      {
        source: "/ops-api/:path*",
        destination: `${backendUrl}/ops/:path*`,
      },
    ];
  },
};

export default nextConfig;
