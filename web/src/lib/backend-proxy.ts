// 운영에서는 Caddy가 /api/v1/* 를 backend:8080으로 직결하므로 web/src/app/api/v1/**의
// route handler(이 함수를 호출하는 쪽)는 도달하지 않는다. 이 프록시는 `npm run dev`로
// Next.js만 단독 실행할 때(Caddy 없이) 백엔드를 호출하기 위한 로컬 개발용 폴백이다.
const backendBaseUrl =
  process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

const passthroughHeaders = [
  "cache-control",
  "content-type",
  "etag",
  "last-modified",
  "retry-after",
  "x-ratelimit-limit",
  "x-ratelimit-remaining",
  "x-request-id",
] as const;

function copyHeaders(source: Headers): Headers {
  const headers = new Headers();
  for (const headerName of passthroughHeaders) {
    const headerValue = source.get(headerName);
    if (headerValue) {
      headers.set(headerName, headerValue);
    }
  }
  return headers;
}

export async function proxyBackend(
  path: string,
  init?: RequestInit
): Promise<Response> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 5000);
  try {
    const res = await fetch(`${backendBaseUrl}${path}`, {
      ...init,
      signal: controller.signal,
      cache: "no-store",
    });
    const body = res.status === 204 ? null : await res.arrayBuffer();
    return new Response(body, {
      status: res.status,
      headers: copyHeaders(res.headers),
    });
  } catch {
    return Response.json(
      { code: "BACKEND_UNAVAILABLE", message: "백엔드 요청을 처리하지 못했습니다." },
      { status: 502 }
    );
  } finally {
    clearTimeout(timer);
  }
}
