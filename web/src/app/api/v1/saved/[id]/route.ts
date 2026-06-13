import { NextRequest, NextResponse } from "next/server";

const backendBaseUrl = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

export async function DELETE(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const deviceToken = req.headers.get("X-Device-Token") ?? "";
    const res = await fetch(`${backendBaseUrl}/api/v1/saved/${id}`, {
      method: "DELETE",
      headers: { "X-Device-Token": deviceToken },
      cache: "no-store",
    });
    if (res.status === 204) {
      return new NextResponse(null, { status: 204 });
    }
    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json({ message: "번호를 삭제하지 못했습니다." }, { status: 502 });
  }
}
