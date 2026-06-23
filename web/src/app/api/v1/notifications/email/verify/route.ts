// 로컬 개발용 프록시 (운영: Caddy가 /api/v1/* → backend:8080 직결)
import { NextRequest } from "next/server";
import { proxyBackend } from "@/lib/backend-proxy";

export async function GET(req: NextRequest) {
  const token = req.nextUrl.searchParams.get("token") ?? "";
  return proxyBackend(`/api/v1/notifications/email/verify?token=${encodeURIComponent(token)}`);
}
