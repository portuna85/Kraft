import { NextRequest, NextResponse } from "next/server";
import { THEME_INIT_SCRIPT, buildWebsiteJsonLd, buildFaqPageJsonLd } from "@/lib/csp-inline-scripts";

const opsAllowedHost = process.env.KRAFT_OPS_ALLOWED_HOST;
const publicBaseUrl = process.env.KRAFT_PUBLIC_BASE_URL ?? "http://localhost";

// 루트 레이아웃의 두 인라인 스크립트(테마 초기화, WebSite JSON-LD)는 배포 수명 동안
// 내용이 고정이므로 nonce 대신 sha256 해시로 허용한다. 매 요청 nonce 발급을 없애야
// ISR/force-static 페이지가 동적 렌더링으로 강제되지 않는다.
let staticScriptHashesPromise: Promise<string[]> | null = null;

async function sha256Base64(text: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return Buffer.from(digest).toString("base64");
}

function getStaticScriptHashes(): Promise<string[]> {
  if (!staticScriptHashesPromise) {
    staticScriptHashesPromise = Promise.all([
      sha256Base64(THEME_INIT_SCRIPT),
      sha256Base64(JSON.stringify(buildWebsiteJsonLd(publicBaseUrl))),
      sha256Base64(JSON.stringify(buildFaqPageJsonLd())),
    ]);
  }
  return staticScriptHashesPromise;
}

function generateNonce(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return Buffer.from(array).toString("base64");
}

function buildCsp(staticHashes: string[], nonce?: string): string {
  const isDev = process.env.NODE_ENV !== "production";
  const scriptSources = staticHashes.map((hash) => `'sha256-${hash}'`);
  if (nonce) {
    scriptSources.push(`'nonce-${nonce}'`);
  }
  if (isDev) {
    scriptSources.push("'unsafe-eval'");
  }
  return [
    `default-src 'self'`,
    `script-src 'self' ${scriptSources.join(" ")}`,
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

export async function proxy(req: NextRequest) {
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

  const staticHashes = await getStaticScriptHashes();

  // /rounds, /rounds/[round]만 회차별로 내용이 달라지는 JSON-LD를 인라인 렌더링하므로
  // 이 경로에서만 per-request nonce를 발급한다. 다른 경로는 정적 해시만으로 충분해
  // 페이지 트리에서 headers()를 호출하지 않아도 되고, 그 결과 ISR/정적 캐시가 살아난다.
  const needsNonce = pathname === "/rounds" || pathname.startsWith("/rounds/");
  const nonce = needsNonce ? generateNonce() : undefined;
  const csp = buildCsp(staticHashes, nonce);

  const requestHeaders = new Headers(req.headers);
  if (nonce) {
    requestHeaders.set("x-nonce", nonce);
  }

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", csp);
  if (nonce) {
    response.headers.set("x-nonce", nonce);
  }

  return response;
}

export const config = {
  matcher: [
    "/ops/:path*",
    "/((?!_next/static|_next/image|favicon\\.ico).*)",
  ],
};
