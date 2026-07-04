import { NextRequest } from "next/server";
import { proxyBackend } from "@/lib/backend-proxy";

export async function GET(req: NextRequest) {
  const round = req.nextUrl.searchParams.get("round") ?? "latest";
  return proxyBackend(`/api/v1/saved/matches?round=${encodeURIComponent(round)}`, {
    headers: { "X-Device-Token": req.headers.get("X-Device-Token") ?? "" },
  });
}
