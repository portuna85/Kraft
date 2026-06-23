import { NextRequest, NextResponse } from "next/server";

const opsAllowedHost = process.env.KRAFT_OPS_ALLOWED_HOST;

// Next.js App Router는 CSP에 nonce가 있을 때만 자신이 스트리밍하는 RSC
// 하이드레이션용 inline script에도 그 nonce를 자동으로 붙여준다(공식 문서 패턴).
// sha256 해시만으로는 우리가 작성한 inline script(테마 초기화, JSON-LD)는 허용되지만
// Next가 내부적으로 주입하는, 페이지마다 내용이 달라지는 RSC 스크립트는 허용할 수 없어
// 모든 페이지에서 하이드레이션이 CSP에 막힌다(실측: /recommend, /saved 등에서 확인).
// 그래서 nonce는 전 경로에 매 요청 발급한다 — ISR/force-static 회복은 보류.
function generateNonce(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return Buffer.from(array).toString("base64");
}

function buildCsp(nonce: string): string {
  const isDev = process.env.NODE_ENV !== "production";
  return [
    `default-src 'self'`,
    `script-src 'self' 'nonce-${nonce}'${isDev ? " 'unsafe-eval'" : ""}`,
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

export function proxy(req: NextRequest) {
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
