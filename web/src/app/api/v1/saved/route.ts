import { NextRequest } from "next/server";
import { proxyBackend } from "@/lib/backend-proxy";

export async function GET(req: NextRequest) {
  return proxyBackend("/api/v1/saved", {
    headers: { "X-Device-Token": req.headers.get("X-Device-Token") ?? "" },
  });
}

export async function POST(req: NextRequest) {
  const body = await req.text();
  return proxyBackend("/api/v1/saved", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Device-Token": req.headers.get("X-Device-Token") ?? "",
    },
    body,
  });
}
