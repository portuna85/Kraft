const backendBaseUrl =
  process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

async function safeJson(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
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
    if (res.status === 204) {
      return new Response(null, { status: 204 });
    }
    return Response.json(await safeJson(res), { status: res.status });
  } catch {
    return Response.json(
      { code: "BACKEND_UNAVAILABLE", message: "백엔드 요청에 실패했습니다." },
      { status: 502 }
    );
  } finally {
    clearTimeout(timer);
  }
}
