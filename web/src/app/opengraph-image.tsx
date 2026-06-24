import { ImageResponse } from "next/og";
import { getOgFontConfig } from "@/lib/og-font";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const BALLS = [
  { n: 3,  bg: "#f5c842", fg: "#1d1a17" },
  { n: 11, bg: "#3a7d44", fg: "#ffffff" },
  { n: 19, bg: "#c94f24", fg: "#ffffff" },
  { n: 28, bg: "#3a5fa0", fg: "#ffffff" },
  { n: 34, bg: "#c94f24", fg: "#ffffff" },
  { n: 42, bg: "#7a7068", fg: "#ffffff" },
];

export default async function OgImage() {
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
        <div
          style={{
            position: "absolute",
            top: -140,
            right: -140,
            width: 520,
            height: 520,
            borderRadius: "50%",
            background: "rgba(201, 79, 36, 0.07)",
            display: "flex",
          }}
        />
        <div
          style={{
            position: "absolute",
            bottom: -100,
            left: -100,
            width: 380,
            height: 380,
            borderRadius: "50%",
            background: "rgba(201, 79, 36, 0.05)",
            display: "flex",
          }}
        />

        <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 40 }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: 14,
              background: "#c94f24",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#fff",
              fontSize: 28,
              fontWeight: 700,
            }}
          >
            K
          </div>
          <span style={{ fontSize: 42, fontWeight: 700, color: "#1d1a17", letterSpacing: -1 }}>
            KRAFT Lotto
          </span>
        </div>

        <div style={{ display: "flex", gap: 18, marginBottom: 44 }}>
          {BALLS.map((ball) => (
            <div
              key={ball.n}
              style={{
                width: 92,
                height: 92,
                borderRadius: "50%",
                background: ball.bg,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: ball.fg,
                fontSize: 34,
                fontWeight: 700,
                boxShadow: "0 8px 24px rgba(0,0,0,0.16)",
              }}
            >
              {ball.n}
            </div>
          ))}
        </div>

        <div style={{ display: "flex", fontSize: 30, fontWeight: 700, color: "#1d1a17", marginBottom: 14, letterSpacing: -0.5 }}>
          로또 6/45 당첨 번호 · 통계 · 번호 추천
        </div>
        <div style={{ display: "flex", fontSize: 22, color: "#5e564c" }}>kraft.io.kr</div>
      </div>
    ),
    { ...size, fonts },
  );
}
