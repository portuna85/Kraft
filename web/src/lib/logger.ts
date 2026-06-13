import pino from "pino";
import { join } from "path";
import createStream from "pino-rotating-file-stream";
import { mkdirSync } from "fs";

const isDev = process.env.NODE_ENV !== "production";

const logDir = process.env.KRAFT_LOG_PATH
  ? join(process.env.KRAFT_LOG_PATH, "web")
  : join(process.cwd(), "logs", "web");

function buildStream() {
  if (isDev) return process.stdout;

  mkdirSync(logDir, { recursive: true });
  const rotating = createStream({
    filename: "web.log",
    interval: "1d",
    rotate: 30,
    path: logDir,
    compress: "gzip",
  });
  return pino.multistream([{ stream: process.stdout }, { stream: rotating }]);
}

const logger = pino(
  {
    level: isDev ? "debug" : "info",
    timestamp: pino.stdTimeFunctions.isoTime,
    base: { service: "kraft-web" },
    formatters: {
      level(label) {
        return { level: label.toUpperCase() };
      },
    },
  },
  buildStream()
);

export default logger;
