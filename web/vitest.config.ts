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
      exclude: ["**/node_modules/**", "**/e2e/**", "**/*.config.*", "src/__tests__/**", "src/lib/api.ts", "src/lib/logger.ts"],
      thresholds: {
        lines: 80,
        statements: 80,
        functions: 80,
        branches: 70,
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
