import { ImageResponse } from "next/og";
import { getOgFontConfig } from "@/lib/og-font-node";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const SAMPLE = [
  { n: 34, pct: 82 },
  { n: 7,  pct: 74 },
  { n: 27, pct: 68 },
  { n: 15, pct: 61 },
  { n: 43, pct: 55 },
  { n: 2,  pct: 47 },
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

        <div style={{ display: "flex", fontSize: 48, fontWeight: 700, color: "#1d1a17", letterSpacing: -1, marginBottom: 10 }}>번호 출현 통계</div>
        <div style={{ display: "flex", fontSize: 22, color: "#5e564c", marginBottom: 40 }}>1회부터 최신 회차까지 전 번호 출현 빈도 분석</div>

        <div style={{ display: "flex", alignItems: "flex-end", gap: 18 }}>
          {SAMPLE.map(({ n, pct }) => (
            <div key={n} style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
              <div
                style={{
                  width: 52,
                  height: pct * 1.2,
                  background: `rgba(201,79,36,${0.3 + pct / 200})`,
                  borderRadius: "6px 6px 0 0",
                  display: "flex",
                }}
              />
              <div style={{ width: 52, height: 52, borderRadius: "50%", background: "#f5c842", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 20, fontWeight: 700, color: "#1d1a17" }}>
                {n}
              </div>
            </div>
          ))}
        </div>
      </div>
    ),
    { ...size, fonts },
  );
}
