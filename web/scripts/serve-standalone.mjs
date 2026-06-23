// E2E 테스트는 prod와 동일한 `output: standalone` 산출물로 띄운다(Dockerfile과 동일 절차).
// `next start`는 standalone 빌드와 호환되지 않으므로 .next/static, public을
// .next/standalone 아래로 복사한 뒤 standalone server.js를 직접 실행한다.
import { cpSync, existsSync } from "node:fs";
import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const webDir = path.dirname(path.dirname(fileURLToPath(import.meta.url)));
const standaloneDir = path.join(webDir, ".next", "standalone");

if (!existsSync(standaloneDir)) {
  console.error("ERROR: .next/standalone not found. Run `npm run build` first.");
  process.exit(1);
}

cpSync(path.join(webDir, ".next", "static"), path.join(standaloneDir, ".next", "static"), { recursive: true });
cpSync(path.join(webDir, "public"), path.join(standaloneDir, "public"), { recursive: true });

const child = spawn(process.execPath, [path.join(standaloneDir, "server.js")], {
  stdio: "inherit",
  env: { ...process.env, PORT: process.env.PORT ?? "3100" },
});

child.on("exit", (code) => process.exit(code ?? 0));
