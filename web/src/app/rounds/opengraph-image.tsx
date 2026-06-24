import { ImageResponse } from "next/og";
import { getLatestWinningNumber } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import { getOgFontConfig } from "@/lib/og-font-node";

export const runtime = "nodejs";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";
export const revalidate = 3600;

function ballColor(n: number): { bg: string; fg: string } {
  if (n <= 10) return { bg: "#f5c842", fg: "#1d1a17" };
  if (n <= 20) return { bg: "#3a5fa0", fg: "#ffffff" };
  if (n <= 30) return { bg: "#c94f24", fg: "#ffffff" };
  if (n <= 40) return { bg: "#7a7068", fg: "#ffffff" };
  return { bg: "#3a7d44", fg: "#ffffff" };
}

const BALL = 94;

export default async function Image() {
  const [latest, { fonts, fontFamily }] = await Promise.all([
    getLatestWinningNumber().catch(() => null),
    getOgFontConfig(),
  ]);

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

        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 24 }}>
          <div style={{ width: 38, height: 38, borderRadius: 10, background: "#c94f24", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 19, fontWeight: 700 }}>K</div>
          <span style={{ fontSize: 26, fontWeight: 700, color: "#1d1a17", letterSpacing: -0.5 }}>KRAFT Lotto</span>
        </div>

        {latest ? (
          <>
            <div style={{ display: "flex", fontSize: 22, color: "#c94f24", fontWeight: 700, marginBottom: 8 }}>최신 당첨 결과</div>
            <div style={{ display: "flex", fontSize: 48, fontWeight: 700, color: "#1d1a17", letterSpacing: -1, marginBottom: 6 }}>
              {`제${latest.round}회 당첨 번호`}
            </div>
            <div style={{ display: "flex", fontSize: 22, color: "#5e564c", marginBottom: 34 }}>
              {`${formatDrawDate(latest.drawDate)} 추첨`}
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 34 }}>
              {latest.numbers.map((n) => {
                const { bg, fg } = ballColor(n);
                return (
                  <div key={n} style={{ width: BALL, height: BALL, borderRadius: "50%", background: bg, display: "flex", alignItems: "center", justifyContent: "center", color: fg, fontSize: 32, fontWeight: 700, boxShadow: "0 6px 20px rgba(0,0,0,0.15)" }}>
                    {n}
                  </div>
                );
              })}
              <div style={{ fontSize: 28, color: "#9e9086", display: "flex", alignItems: "center", justifyContent: "center", width: 32 }}>+</div>
              <div style={{ width: BALL, height: BALL, borderRadius: "50%", background: ballColor(latest.bonusNumber).bg, display: "flex", alignItems: "center", justifyContent: "center", color: ballColor(latest.bonusNumber).fg, fontSize: 32, fontWeight: 700, boxShadow: "0 6px 20px rgba(0,0,0,0.15)", border: "3px dashed rgba(0,0,0,0.18)" }}>
                {latest.bonusNumber}
              </div>
            </div>

            <div style={{ display: "flex", fontSize: 24, color: "#c94f24", fontWeight: 700 }}>
              {`1등 당첨금 ${formatCurrency(latest.firstPrizeAmount)}`}
            </div>
          </>
        ) : (
          <>
            <div style={{ display: "flex", fontSize: 40, fontWeight: 700, color: "#1d1a17", letterSpacing: -1, marginBottom: 14 }}>
              회차 결과 · 전체 목록
            </div>
            <div style={{ display: "flex", fontSize: 24, color: "#5e564c" }}>
              로또 6/45 모든 회차를 한눈에
            </div>
          </>
        )}
      </div>
    ),
    { ...size, fonts },
  );
}
