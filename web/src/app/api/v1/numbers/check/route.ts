import { NextRequest } from "next/server";
import { proxyBackend } from "@/lib/backend-proxy";

export async function GET(req: NextRequest) {
  const numbers = req.nextUrl.searchParams.getAll("numbers");
  const query = numbers.map((n) => `numbers=${encodeURIComponent(n)}`).join("&");
  return proxyBackend(`/api/v1/numbers/check${query ? `?${query}` : ""}`);
}
