import { NextRequest, NextResponse } from "next/server";

const opsAllowedHost = process.env.KRAFT_OPS_ALLOWED_HOST;

function generateNonce(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return Buffer.from(array).toString("base64");
}

function buildCsp(nonce: string): string {
  return [
    `default-src 'self'`,
    `script-src 'self' 'nonce-${nonce}'`,
    `style-src 'self' 'unsafe-inline'`,
    `img-src 'self' data:`,
    `font-src 'self'`,
    `connect-src 'self'`,
    `object-src 'none'`,
    `base-uri 'self'`,
    `form-action 'self'`,
    `frame-ancestors 'none'`,
  ].join("; ");
}

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

  const nonce = generateNonce();
  const csp = buildCsp(nonce);

  const requestHeaders = new Headers(req.headers);
  requestHeaders.set("x-nonce", nonce);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", csp);
  response.headers.set("x-nonce", nonce);

  return response;
}

export const config = {
  matcher: [
    "/ops/:path*",
    "/((?!_next/static|_next/image|favicon\\.ico).*)",
  ],
};
