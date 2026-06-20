import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

describe("proxyBackend", () => {
  const originalFetch = global.fetch;

  beforeEach(() => {
    vi.resetModules();
  });

  afterEach(() => {
    global.fetch = originalFetch;
    vi.restoreAllMocks();
  });

  it("preserves selected backend headers and body", async () => {
    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: {
          "Content-Type": "application/json",
          "Cache-Control": "public, max-age=60, must-revalidate",
          ETag: "\"abc123\"",
          "X-Request-Id": "req-1",
          "X-RateLimit-Limit": "120",
          "X-RateLimit-Remaining": "119",
        },
      })
    ) as typeof fetch;

    const { proxyBackend } = await import("@/lib/backend-proxy");
    const response = await proxyBackend("/api/v1/stats/frequency");

    expect(response.status).toBe(200);
    expect(response.headers.get("content-type")).toBe("application/json");
    expect(response.headers.get("cache-control")).toContain("max-age=60");
    expect(response.headers.get("etag")).toBe("\"abc123\"");
    expect(response.headers.get("x-request-id")).toBe("req-1");
    expect(response.headers.get("x-ratelimit-limit")).toBe("120");
    expect(response.headers.get("x-ratelimit-remaining")).toBe("119");
    await expect(response.json()).resolves.toEqual({ ok: true });
  });

  it("returns a 502 json response when the backend request fails", async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error("timeout")) as typeof fetch;

    const { proxyBackend } = await import("@/lib/backend-proxy");
    const response = await proxyBackend("/api/v1/stats/frequency");

    expect(response.status).toBe(502);
    await expect(response.json()).resolves.toMatchObject({
      code: "BACKEND_UNAVAILABLE",
    });
  });
});
