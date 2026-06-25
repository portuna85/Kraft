import { describe, expect, it, beforeEach, vi } from "vitest";

describe("getDeviceToken", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.resetModules();
  });

  it("returns empty string in SSR context (window undefined)", async () => {
    // Simulate SSR by temporarily hiding window
    const win = globalThis.window;
    // @ts-expect-error — intentional SSR simulation
    delete globalThis.window;
    const { getDeviceToken } = await import("@/lib/device-token");
    expect(getDeviceToken()).toBe("");
    globalThis.window = win;
  });

  it("creates and caches a token on first call", async () => {
    const { getDeviceToken } = await import("@/lib/device-token");
    const token = getDeviceToken();
    expect(token).toBeTruthy();
    expect(token.length).toBeGreaterThanOrEqual(32);
    // Second call must return the same token (cached in localStorage)
    expect(getDeviceToken()).toBe(token);
  });

  it("returns the persisted token on subsequent module loads", async () => {
    const { getDeviceToken } = await import("@/lib/device-token");
    const first = getDeviceToken();

    vi.resetModules();
    const { getDeviceToken: getDeviceToken2 } = await import("@/lib/device-token");
    expect(getDeviceToken2()).toBe(first);
  });

  it("falls back to createRandomToken when crypto.randomUUID is unavailable", async () => {
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
