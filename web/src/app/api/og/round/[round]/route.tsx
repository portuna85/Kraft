import { ImageResponse } from "next/og";
import type { NextRequest } from "next/server";

// RSC(generateMetadata)가 쿼리 파라미터로 ball 데이터를 넘기므로
// API route에서 별도 백엔드 fetch 없이 렌더링 가능.
export const runtime = "edge";

const SIZE = { width: 1200, height: 630 };
const BALL = 94;

function ballColor(n: number): { bg: string; fg: string } {
  if (n <= 10) return { bg: "#f5c842", fg: "#1d1a17" };
  if (n <= 20) return { bg: "#3a5fa0", fg: "#ffffff" };
  if (n <= 30) return { bg: "#c94f24", fg: "#ffffff" };
  if (n <= 40) return { bg: "#7a7068", fg: "#ffffff" };
  return { bg: "#3a7d44", fg: "#ffffff" };
}

function formatDate(value: string): string {
  const [year, month, day] = value.split("-").map(Number);
  const names = ["일", "월", "화", "수", "목", "금", "토"];
  return `${year}년 ${month}월 ${day}일 (${names[new Date(Date.UTC(year, month - 1, day)).getUTCDay()]})`;
}

function formatMoney(value: number): string {
  return `${new Intl.NumberFormat("ko-KR").format(value)}원`;
}

const PLACEHOLDER_BALLS = [
  { bg: "#f5c842", fg: "#1d1a17" },
  { bg: "#3a5fa0", fg: "#ffffff" },
  { bg: "#c94f24", fg: "#ffffff" },
  { bg: "#7a7068", fg: "#ffffff" },
  { bg: "#3a7d44", fg: "#ffffff" },
  { bg: "#c94f24", fg: "#ffffff" },
];

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ round: string }> },
) {
  const { round } = await params;
  const { searchParams } = req.nextUrl;

  const ballsParam = searchParams.get("b");
  const bonusParam = searchParams.get("bo");
  const dateParam = searchParams.get("d");
  const prizeParam = searchParams.get("p");

  const numbers = ballsParam ? ballsParam.split(",").map(Number).filter(n => n > 0 && n <= 45) : null;
  const bonusNumber = bonusParam ? Number(bonusParam) : 0;
  const drawDate = dateParam ?? "";
  const firstPrizeAmount = prizeParam ? Number(prizeParam) : 0;
  const roundNum = Number(round);

  const hasData = numbers && numbers.length === 6 && bonusNumber > 0;

  const body = hasData ? (
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

      <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 24 }}>
        <div style={{ width: 38, height: 38, borderRadius: 10, background: "#c94f24", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 19, fontWeight: 800 }}>K</div>
        <span style={{ fontSize: 26, fontWeight: 800, color: "#1d1a17", letterSpacing: -0.5 }}>KRAFT Lotto</span>
      </div>

      <div style={{ display: "flex", fontSize: 48, fontWeight: 800, color: "#1d1a17", letterSpacing: -1, marginBottom: 6 }}>
        제{roundNum}회 당첨 결과
      </div>
      <div style={{ display: "flex", fontSize: 22, color: "#5e564c", marginBottom: 34 }}>
        {drawDate ? `${formatDate(drawDate)} 추첨` : ""}
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 34 }}>
        {numbers.map((n) => {
          const { bg, fg } = ballColor(n);
          return (
            <div key={n} style={{ width: BALL, height: BALL, borderRadius: "50%", background: bg, display: "flex", alignItems: "center", justifyContent: "center", color: fg, fontSize: 32, fontWeight: 800, boxShadow: "0 6px 20px rgba(0,0,0,0.15)" }}>
              {n}
            </div>
          );
        })}
        <div style={{ fontSize: 28, color: "#9e9086", display: "flex", alignItems: "center", justifyContent: "center", width: 32 }}>+</div>
        <div style={{ width: BALL, height: BALL, borderRadius: "50%", background: ballColor(bonusNumber).bg, display: "flex", alignItems: "center", justifyContent: "center", color: ballColor(bonusNumber).fg, fontSize: 32, fontWeight: 800, boxShadow: "0 6px 20px rgba(0,0,0,0.15)", border: "3px dashed rgba(0,0,0,0.18)" }}>
          {bonusNumber}
        </div>
      </div>

      {firstPrizeAmount > 0 && (
        <div style={{ display: "flex", fontSize: 24, color: "#c94f24", fontWeight: 700 }}>
          1등 당첨금 {formatMoney(firstPrizeAmount)}
        </div>
      )}
    </div>
  ) : (
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
      <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 28 }}>
        <div style={{ width: 38, height: 38, borderRadius: 10, background: "#c94f24", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 19, fontWeight: 800 }}>K</div>
        <span style={{ fontSize: 26, fontWeight: 800, color: "#1d1a17", letterSpacing: -0.5 }}>KRAFT Lotto</span>
      </div>
      <div style={{ display: "flex", fontSize: 52, fontWeight: 800, color: "#1d1a17", letterSpacing: -1, marginBottom: 36 }}>
        제{round}회 당첨 결과
      </div>
      <div style={{ display: "flex", gap: 16 }}>
        {PLACEHOLDER_BALLS.map((ball, i) => (
          <div key={i} style={{ width: 92, height: 92, borderRadius: "50%", background: ball.bg, display: "flex", alignItems: "center", justifyContent: "center", color: ball.fg, fontSize: 34, fontWeight: 800, boxShadow: "0 6px 20px rgba(0,0,0,0.15)" }}>?</div>
        ))}
      </div>
    </div>
  );

  return new ImageResponse(body, SIZE);
}
