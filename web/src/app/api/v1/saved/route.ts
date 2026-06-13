import { NextRequest, NextResponse } from "next/server";

const backendBaseUrl = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

export async function GET(req: NextRequest) {
  try {
    const deviceToken = req.headers.get("X-Device-Token") ?? "";
    const res = await fetch(`${backendBaseUrl}/api/v1/saved`, {
      headers: { "X-Device-Token": deviceToken },
      cache: "no-store",
    });
    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json({ message: "저장함을 불러오지 못했습니다." }, { status: 502 });
  }
}

export async function POST(req: NextRequest) {
  try {
    const deviceToken = req.headers.get("X-Device-Token") ?? "";
    const body = await req.json();
    const res = await fetch(`${backendBaseUrl}/api/v1/saved`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Device-Token": deviceToken,
      },
      body: JSON.stringify(body),
      cache: "no-store",
    });
    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json({ message: "번호를 저장하지 못했습니다." }, { status: 502 });
  }
}
