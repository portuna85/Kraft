import { describe, expect, it, beforeEach, vi } from "vitest";

describe("getDeviceToken", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetModules();
  });

  it("SSR 환경(window 미정의)에서는 빈 문자열을 반환한다", async () => {
    // Simulate SSR by temporarily hiding window
    const win = globalThis.window;
    // @ts-expect-error — intentional SSR simulation
    delete globalThis.window;
    const { getDeviceToken } = await import("@/lib/device-token");
    expect(getDeviceToken()).toBe("");
    globalThis.window = win;
  });

  it("최초 호출 시 토큰을 생성하고 캐시한다", async () => {
    const { getDeviceToken } = await import("@/lib/device-token");
    const token = getDeviceToken();
    expect(token).toBeTruthy();
    expect(token.length).toBeGreaterThanOrEqual(32);
    // Second call must return the same token (cached in localStorage)
    expect(getDeviceToken()).toBe(token);
  });

  it("모듈을 다시 로드해도 저장된 토큰을 반환한다", async () => {
    const { getDeviceToken } = await import("@/lib/device-token");
    const first = getDeviceToken();

    vi.resetModules();
    const { getDeviceToken: getDeviceToken2 } = await import("@/lib/device-token");
    expect(getDeviceToken2()).toBe(first);
  });

  it("crypto.randomUUID를 쓸 수 없으면 createRandomToken으로 대체한다", async () => {
    const originalDescriptor = Object.getOwnPropertyDescriptor(crypto, "randomUUID");
    Object.defineProperty(crypto, "randomUUID", { value: undefined, configurable: true, writable: true });
    try {
      const { getDeviceToken } = await import("@/lib/device-token");
      const token = getDeviceToken();
      expect(token).toHaveLength(64); // 32 bytes × 2 hex chars
    } finally {
      if (originalDescriptor) {
        Object.defineProperty(crypto, "randomUUID", originalDescriptor);
      }
    }
  });
});
