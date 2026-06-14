import { NextRequest, NextResponse } from "next/server";

const backendBaseUrl = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

export async function GET(req: NextRequest) {
  try {
    const limit = req.nextUrl.searchParams.get("limit");
    const url = `${backendBaseUrl}/api/v1/stats/frequency${limit ? `?limit=${limit}` : ""}`;
    const res = await fetch(url, { cache: "no-store" });
    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json({ message: "빈도 통계를 불러오지 못했습니다." }, { status: 502 });
  }
}
