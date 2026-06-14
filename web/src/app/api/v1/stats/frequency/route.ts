import { NextRequest } from "next/server";
import { proxyBackend } from "@/lib/backend-proxy";

export async function GET(req: NextRequest) {
  const limit = req.nextUrl.searchParams.get("limit");
  const path = `/api/v1/stats/frequency${limit ? `?limit=${encodeURIComponent(limit)}` : ""}`;
  return proxyBackend(path);
}
