import { NextRequest, NextResponse } from "next/server";

const opsAllowedHost = process.env.KRAFT_OPS_ALLOWED_HOST;

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl;

  if (pathname.startsWith("/ops")) {
    if (opsAllowedHost) {
      const host = req.headers.get("host") ?? "";
      const hostname = host.split(":")[0];
      if (hostname !== opsAllowedHost) {
        return NextResponse.rewrite(new URL("/not-found", req.url), { status: 404 });
      }
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/ops/:path*"],
};
