import { ImageResponse } from "next/og";
import { getOgFontConfig } from "@/lib/og-font-node";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const BALLS = [
  { n: "?", bg: "#f5c842", fg: "#1d1a17" },
  { n: "?", bg: "#3a5fa0", fg: "#ffffff" },
  { n: "?", bg: "#c94f24", fg: "#ffffff" },
  { n: "?", bg: "#3a7d44", fg: "#ffffff" },
  { n: "?", bg: "#7a7068", fg: "#ffffff" },
  { n: "?", bg: "#c94f24", fg: "#ffffff" },
];

export default async function Image() {
  const { fonts, fontFamily } = await getOgFontConfig();

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
          fontFamily,
          position: "relative",
        }}
      >
        <div style={{ position: "absolute", top: -120, right: -120, width: 480, height: 480, borderRadius: "50%", background: "rgba(201,79,36,0.07)", display: "flex" }} />
        <div style={{ position: "absolute", bottom: -80, left: -80, width: 340, height: 340, borderRadius: "50%", background: "rgba(201,79,36,0.05)", display: "flex" }} />

        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 28 }}>
          <div style={{ width: 38, height: 38, borderRadius: 10, background: "#c94f24", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 19, fontWeight: 700 }}>K</div>
          <span style={{ fontSize: 26, fontWeight: 700, color: "#1d1a17", letterSpacing: -0.5 }}>KRAFT Lotto</span>
        </div>

        <div style={{ display: "flex", fontSize: 48, fontWeight: 700, color: "#1d1a17", letterSpacing: -1, marginBottom: 10 }}>번호 추천</div>
        <div style={{ display: "flex", fontSize: 22, color: "#5e564c", marginBottom: 40 }}>보안 난수로 생성하는 나만의 로또 조합</div>

        <div style={{ display: "flex", gap: 16 }}>
          {BALLS.map((ball, i) => (
            <div
              key={i}
              style={{
                width: 92,
                height: 92,
                borderRadius: "50%",
                background: ball.bg,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: ball.fg,
                fontSize: 38,
                fontWeight: 700,
                boxShadow: "0 6px 20px rgba(0,0,0,0.14)",
                opacity: 0.85 + i * 0.025,
              }}
            >
              {ball.n}
            </div>
          ))}
        </div>
      </div>
    ),
    { ...size, fonts },
  );
}
