import type { Metadata } from "next";
import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { PrizeTable } from "@/components/prize-table";
import { getLatestWinningNumber, type WinningNumber } from "@/lib/api";
import { formatDrawDate } from "@/lib/format";
import logger from "@/lib/logger";

export const revalidate = 60;

export async function generateMetadata(): Promise<Metadata> {
  try {
    const latest = await getLatestWinningNumber();
    return {
      title: `${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      description: `${latest.round}회 로또 6/45 당첨 번호 ${[...latest.numbers].join(", ")} 보너스 ${latest.bonusNumber}. 당첨 결과 조회와 번호 추천을 제공합니다.`,
      alternates: { canonical: "/" },
      openGraph: {
        title: `${latest.round}회 로또 당첨번호 | KRAFT Lotto`,
        description: `${latest.round}회 당첨 번호 ${[...latest.numbers, latest.bonusNumber].join(", ")}`,
        url: "/",
      },
    };
  } catch {
    return {
      title: "KRAFT Lotto | 로또 6/45 결과와 번호 추천",
      description: "로또 6/45 최신 당첨 번호 조회, 회차 검색, 번호 추천, 통계 분석 기능을 제공합니다.",
      alternates: { canonical: "/" },
    };
  }
}

export default async function HomePage() {
  let latest: WinningNumber | null = null;

  try {
    latest = await getLatestWinningNumber();
  } catch (error) {
    logger.warn({ err: error }, "최신 당첨번호 조회 실패");
  }

  return (
    <div className="section-stack">
      {latest ? (
        <section className="panel result-panel hero-panel">
          <p className="eyebrow">최신 결과</p>
          <h1 className="result-title">
            {latest.round}회 당첨 결과 <span className="result-date">({formatDrawDate(latest.drawDate)})</span>
          </h1>
          <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
          <PrizeTable firstPrizeAmount={latest.firstPrizeAmount} secondPrize={latest.secondPrize} />
        </section>
      ) : (
        <section className="panel result-panel hero-panel">
          <p className="eyebrow">최신 결과</p>
          <h1 className="result-title">최신 결과를 준비 중입니다</h1>
          <p className="page-subtitle">잠시 후 다시 확인해 주세요.</p>
        </section>
      )}

      <section className="grid grid-3 home-shortcuts">
        <Link href="/rounds" className="stat-card stat-link">
          <p className="eyebrow">결과 조회</p>
          <h3>최신 결과</h3>
          <span className="stat-link-cta">바로 보기</span>
        </Link>

        <Link href="/recommend" className="stat-card stat-link">
          <p className="eyebrow">번호 추천</p>
          <h3>추천 조합</h3>
          <span className="stat-link-cta">추천 받기</span>
        </Link>

        <Link href="/frequency" className="stat-card stat-link">
          <p className="eyebrow">출현 통계</p>
          <h3>번호 통계</h3>
          <span className="stat-link-cta">통계 보기</span>
        </Link>
      </section>
    </div>
  );
}
