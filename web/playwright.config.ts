import { defineConfig, devices } from "@playwright/test";

// 백엔드/DB 없이도 돌아가는 스모크 E2E. 클라이언트 컴포넌트(/recommend, /saved)는
// 브라우저 fetch를 라우트 모킹으로 가로채 검증하고, 서버 컴포넌트 페이지(/, /rounds 등)는
// 백엔드 fetch 실패 시 이미 구현된 폴백 UI(예: "준비 중입니다")로 렌더링된다.
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: "http://127.0.0.1:3100",
    trace: "retain-on-failure",
  },
  projects: [
    { name: "chromium",      use: { ...devices["Desktop Chrome"] } },
    { name: "Mobile Chrome", use: { ...devices["Pixel 5"] } },
    { name: "Tablet",        use: { browserName: "chromium", ...devices["iPad (gen 7)"] } },
  ],
  webServer: {
    // standalone 빌드 산출물(Dockerfile과 동일)을 그대로 띄운다. `npm run build`를
    // 먼저 실행해 .next/standalone이 있어야 한다.
    command: "npm run e2e:serve",
    url: "http://127.0.0.1:3100",
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
    env: {
      // 백엔드 없이도 빠르게 실패하도록 즉시 거부되는 루프백 포트를 가리킨다.
      // 서버 컴포넌트 페이지는 이미 구현된 폴백 UI로 렌더링된다.
      KRAFT_BACKEND_INTERNAL_URL: "http://127.0.0.1:59999",
      KRAFT_PUBLIC_BASE_URL: "http://127.0.0.1:3100",
      // undo 창을 200ms로 단축해 DELETE 타이머 테스트가 5초를 기다리지 않도록 한다.
      NEXT_PUBLIC_UNDO_WINDOW_MS: "200",
    },
  },
});
