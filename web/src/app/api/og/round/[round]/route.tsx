import { ImageResponse } from "next/og";
import type { NextRequest } from "next/server";

// Edge runtime: WASM 기반 ImageResponse 사용 → standalone 컨테이너에서 native 바이너리 불필요
export const runtime = "edge";

const SIZE = { width: 1200, height: 630 };
const BALL = 94;
const BACKEND = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";

function ballColor(n: number): { bg: string; fg: string } {
  if (n <= 10) return { bg: "#f5c842", fg: "#1d1a17" };
  if (n <= 20) return { bg: "#3a5fa0", fg: "#ffffff" };
  if (n <= 30) return { bg: "#c94f24", fg: "#ffffff" };
  if (n <= 40) return { bg: "#7a7068", fg: "#ffffff" };
  return { bg: "#3a7d44", fg: "#ffffff" };
}

// Edge runtime은 Intl.DateTimeFormat({ timeZone }) 미지원 → Date.UTC 기반 구현
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

type RoundData = {
  round: number;
  drawDate: string;
  numbers: number[];
  bonusNumber: number;
  firstPrizeAmount: number;
};

async function fetchRound(round: number): Promise<RoundData | null> {
  try {
    const controller = new AbortController();
    const id = setTimeout(() => controller.abort(), 4000);
    const res = await fetch(`${BACKEND}/api/v1/rounds/${round}`, { signal: controller.signal });
    clearTimeout(id);
    if (!res.ok) return null;
    return res.json() as Promise<RoundData>;
  } catch {
    return null;
  }
}

export async function GET(
  _req: NextRequest,
  { params }: { params: Promise<{ round: string }> },
) {
  const { round } = await params;
  const data = await fetchRound(Number(round));

  const body = data ? (
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

      <div style={{ fontSize: 48, fontWeight: 800, color: "#1d1a17", letterSpacing: -1, marginBottom: 6 }}>
        제{data.round}회 당첨 결과
      </div>
      <div style={{ fontSize: 22, color: "#5e564c", marginBottom: 34 }}>
        {formatDate(data.drawDate)} 추첨
      </div>

      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 34 }}>
        {data.numbers.map((n) => {
          const { bg, fg } = ballColor(n);
          return (
            <div key={n} style={{ width: BALL, height: BALL, borderRadius: "50%", background: bg, display: "flex", alignItems: "center", justifyContent: "center", color: fg, fontSize: 32, fontWeight: 800, boxShadow: "0 6px 20px rgba(0,0,0,0.15)" }}>
              {n}
            </div>
          );
        })}
        <div style={{ fontSize: 28, color: "#9e9086", display: "flex", alignItems: "center", justifyContent: "center", width: 32 }}>+</div>
        <div style={{ width: BALL, height: BALL, borderRadius: "50%", background: ballColor(data.bonusNumber).bg, display: "flex", alignItems: "center", justifyContent: "center", color: ballColor(data.bonusNumber).fg, fontSize: 32, fontWeight: 800, boxShadow: "0 6px 20px rgba(0,0,0,0.15)", border: "3px dashed rgba(0,0,0,0.18)" }}>
          {data.bonusNumber}
        </div>
      </div>

      <div style={{ fontSize: 24, color: "#c94f24", fontWeight: 700 }}>
        1등 당첨금 {formatMoney(data.firstPrizeAmount)}
      </div>
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
      <div style={{ fontSize: 52, fontWeight: 800, color: "#1d1a17", letterSpacing: -1, marginBottom: 36 }}>
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
