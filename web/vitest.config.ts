import { defineConfig } from "vitest/config";
import path from "path";

export default defineConfig({
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/__tests__/setup.ts"],
    exclude: ["**/node_modules/**", "**/e2e/**"],
    coverage: {
      provider: "v8",
      // Vitest 4의 v8 provider는 include에 매칭되는 모든 파일을 항상 분모에 포함한다
      // (예전 all:true가 기본 동작이 됨 — 별도 옵션은 더 이상 존재하지 않는다).
      include: ["src/**/*.{ts,tsx}"],
      // src/app/**는 Next.js 페이지·레이아웃·라우트 핸들러 구성 코드로, RSC/스트리밍 등
      // 실제 Next 런타임 없이는 vitest+jsdom으로 의미 있게 단위 테스트하기 어렵다 —
      // e2e(Playwright)가 담당하는 영역이라 커버리지 분모에서 제외한다.
      exclude: ["**/node_modules/**", "**/e2e/**", "**/*.config.*", "src/__tests__/**", "src/lib/api.ts", "src/lib/logger.ts", "src/app/**"],
      // all:true를 켜기 전에는 어떤 테스트도 import하지 않은 파일(예: src/app/** 밖의
      // footer.tsx, json-ld.tsx, round-card-client.tsx 등)이 분모에서 아예 빠져 있어
      // 80/70이 실제보다 부풀려진 숫자였다. all:true로 정직한 분모를 쓰면 현재 실측치는
      // 약 66/61/63/68%(2026-07-21 기준) — 그 미테스트 컴포넌트들에 대한 테스트 작성은
      // 별도 후속 작업(docs/improvement.md F11 참고)이고, 당장은 실측치보다 살짝 낮은
      // 값을 회귀 방지선으로 둔다.
      thresholds: {
        lines: 65,
        statements: 65,
        functions: 60,
        branches: 58,
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
