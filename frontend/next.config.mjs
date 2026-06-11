/** @type {import('next').NextConfig} */
const isProd = process.env.NODE_ENV === 'production'

const nextConfig = {
  output: isProd ? 'export' : undefined,
  trailingSlash: true,
  images: { unoptimized: true },
  async rewrites() {
    if (isProd) return []
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
      {
        source: '/sitemap.xml',
        destination: 'http://localhost:8080/sitemap.xml',
      },
    ]
  },
}

export default nextConfig
