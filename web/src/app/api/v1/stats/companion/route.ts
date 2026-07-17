import { NextRequest } from "next/server";
import { proxyBackend } from "@/lib/backend-proxy";

export async function GET(req: NextRequest) {
  const ball = req.nextUrl.searchParams.get("ball");
  const query = ball ? `?ball=${encodeURIComponent(ball)}` : "";
  return proxyBackend(`/api/v1/stats/companion${query}`);
}
