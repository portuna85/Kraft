import { defineConfig, devices } from "@playwright/test";

// 백엔드/DB 없이도 돌아가는 스모크 E2E. 클라이언트 컴포넌트(/recommend, /saved)는
// 브라우저 fetch를 라우트 모킹으로 가로채 검증하고, 서버 컴포넌트 페이지(/, /frequency 등)는
// 백엔드가 없다는 전제로 에러 경계(error.tsx)·폴백 UI가 정상 렌더되는지를 검증한다
// (stats-family.spec.ts, status.spec.ts 등). responsive.spec.ts의 오버플로 검사도 현재
// 이 상태(에러 화면) 기준이라는 한계가 있다 — docs/improvement.md §6-2, 백엔드 픽스처를
// 도입해 정상 상태까지 커버하려면 테스트별로 백엔드 상태를 분리할 방법이 필요해 별도 작업으로 남겨둔다.
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
      KRAFT_BACKEND_INTERNAL_URL: "http://127.0.0.1:59999",
      KRAFT_PUBLIC_BASE_URL: "http://127.0.0.1:3100",
      // scripts/start-standalone.mjs의 기본 포트(3000, npm start/Docker와 동일)에
      // 의존하지 않도록 E2E 전용 포트를 명시한다.
      PORT: "3100",
    },
  },
});
