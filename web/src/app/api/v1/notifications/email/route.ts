// 로컬 개발용 프록시 (운영: Caddy가 /api/v1/* → backend:8080 직결)
import { NextRequest } from "next/server";
import { proxyBackend } from "@/lib/backend-proxy";

export async function GET(req: NextRequest) {
  return proxyBackend("/api/v1/notifications/email", {
    headers: { "X-Device-Token": req.headers.get("X-Device-Token") ?? "" },
  });
}

export async function POST(req: NextRequest) {
  const body = await req.text();
  return proxyBackend("/api/v1/notifications/email", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Device-Token": req.headers.get("X-Device-Token") ?? "",
    },
    body,
  });
}

export async function DELETE(req: NextRequest) {
  return proxyBackend("/api/v1/notifications/email", {
    method: "DELETE",
    headers: { "X-Device-Token": req.headers.get("X-Device-Token") ?? "" },
  });
}
