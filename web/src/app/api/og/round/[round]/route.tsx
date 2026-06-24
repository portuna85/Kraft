import { ImageResponse } from "next/og";
import type { NextRequest } from "next/server";

// 진단용 최소 버전: 외부 API 없이 완전히 정적인 응답만 반환
export const runtime = "edge";

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ round: string }> },
) {
  const { round } = await params;

  return new ImageResponse(
    (
      <div
        style={{
          width: 1200,
          height: 630,
          background: "#f6f1e8",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: 80,
          fontWeight: 800,
          color: "#c94f24",
        }}
      >
        Round {round}
      </div>
    ),
    { width: 1200, height: 630 },
  );
}
