import { ImageResponse } from "next/og";

export const runtime = "nodejs";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const BALLS = [
  { bg: "#f5c842", fg: "#1d1a17" },
  { bg: "#3a5fa0", fg: "#ffffff" },
  { bg: "#c94f24", fg: "#ffffff" },
  { bg: "#7a7068", fg: "#ffffff" },
  { bg: "#3a7d44", fg: "#ffffff" },
  { bg: "#c94f24", fg: "#ffffff" },
];

export default async function Image({ params }: { params: Promise<{ round: string }> }) {
  const { round } = await params;

  return new ImageResponse(
    (
      <div
        style={{
          width: 1200,
          height: 630,
          background: "linear-gradient(145deg, #f6f1e8 0%, #efe5d2 100%)",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: "sans-serif",
          position: "relative",
        }}
      >
        <div style={{ position: "absolute", top: -120, right: -120, width: 480, height: 480, borderRadius: "50%", background: "rgba(201,79,36,0.07)", display: "flex" }} />
        <div style={{ position: "absolute", bottom: -80, left: -80, width: 340, height: 340, borderRadius: "50%", background: "rgba(201,79,36,0.05)", display: "flex" }} />

        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 28 }}>
          <div style={{ width: 38, height: 38, borderRadius: 10, background: "#c94f24", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 19, fontWeight: 800 }}>K</div>
          <span style={{ fontSize: 26, fontWeight: 800, color: "#1d1a17", letterSpacing: -0.5 }}>KRAFT Lotto</span>
        </div>

        <div style={{ fontSize: 52, fontWeight: 800, color: "#1d1a17", letterSpacing: -1, marginBottom: 36 }}>
          제{round}회 당첨 결과
        </div>

        <div style={{ display: "flex", gap: 16 }}>
          {BALLS.map((ball, i) => (
            <div
              key={i}
              style={{ width: 92, height: 92, borderRadius: "50%", background: ball.bg, display: "flex", alignItems: "center", justifyContent: "center", color: ball.fg, fontSize: 34, fontWeight: 800, boxShadow: "0 6px 20px rgba(0,0,0,0.15)" }}
            >
              ?
            </div>
          ))}
        </div>
      </div>
    ),
    { ...size },
  );
}
