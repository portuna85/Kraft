import { ImageResponse } from "next/og";

export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const ballNumbers = [3, 11, 19, 28, 34, 42];

export default function OgImage() {
  return new ImageResponse(
    (
      <div
        style={{
          background: "#f6f1e8",
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: 32,
        }}
      >
        <div style={{ display: "flex", gap: 20 }}>
          {ballNumbers.map((n) => (
            <div
              key={n}
              style={{
                width: 88,
                height: 88,
                borderRadius: "50%",
                background: "#ffc857",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 36,
                fontWeight: 700,
                color: "#1d1a17",
              }}
            >
              {n}
            </div>
          ))}
        </div>
        <div
          style={{
            fontSize: 76,
            fontWeight: 700,
            color: "#1d1a17",
            letterSpacing: "-2px",
          }}
        >
          KRAFT Lotto
        </div>
        <div style={{ fontSize: 34, color: "#c94f24" }}>
          로또 번호 조회 · 추천 · 저장
        </div>
      </div>
    ),
    { ...size }
  );
}
