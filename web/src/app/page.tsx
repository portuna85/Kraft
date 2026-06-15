import type { Metadata } from "next";
import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { RoundSearchForm } from "@/components/round-search-form";
import { getLatestWinningNumber, type WinningNumber } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import logger from "@/lib/logger";
export const revalidate = 60;

export async function generateMetadata(): Promise<Metadata> {
  try {
    const latest = await getLatestWinningNumber();
    return {
      title: `${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      description: `${latest.round}회 로또 6/45 당첨 번호 ${[...latest.numbers].join(", ")}(보너스 ${latest.bonusNumber}). 당첨금 조회, 번호 추천·저장 기능 제공.`,
      alternates: { canonical: "/" },
      openGraph: {
        title: `${latest.round}회 로또 당첨번호 | KRAFT Lotto`,
        description: `${latest.round}회 당첨 번호 ${[...latest.numbers, latest.bonusNumber].join(", ")}`,
        url: "/",
      },
    };
  } catch {
    return {
      title: "KRAFT Lotto — 로또 6/45 당첨 결과 · 번호 추천",
      description: "로또 6/45 최신 당첨 번호와 당첨금 조회, 번호 추천·저장 기능을 무료로 제공합니다. 동행복권 공식 데이터 기반.",
      alternates: { canonical: "/" },
    };
  }
}

export default async function HomePage() {
  let latest: WinningNumber | null = null;

  try {
    latest = await getLatestWinningNumber();
  } catch (err) {
    logger.warn({ err }, "최신 당첨번호 조회 실패");
  }

  return (
    <div className="grid">
      <section className="hero">
        <div>
          <h1>로또 6/45 결과 조회 · 번호 추천 · 저장</h1>
          <div className="hero-actions">
            <Link href="/latest" className="button">최신 결과</Link>
            <Link href="/recommend" className="button secondary">번호 추천</Link>
            <Link href="/saved" className="button secondary">저장 번호</Link>
          </div>
        </div>

        <aside className="hero-side">
          <p className="eyebrow">가장 최근 추첨</p>
          {latest ? (
            <>
              <h2>{latest.round}회</h2>
              <p className="muted">{formatDrawDate(latest.drawDate)}</p>
              <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
              <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
            </>
          ) : (
            <>
              <p className="muted">최신 당첨 결과를 아직 불러오지 못했습니다.</p>
              <Link href="/rounds" className="button secondary">전체 회차 보러가기</Link>
            </>
          )}
          <RoundSearchForm />
        </aside>
      </section>

      <section className="grid grid-3">
        <Link href="/latest" className="stat-card stat-link">
          <p className="eyebrow">결과 조회</p>
          <h3>최신 결과</h3>
          <span className="stat-link-cta">바로 보기</span>
        </Link>

        <Link href="/recommend" className="stat-card stat-link">
          <p className="eyebrow">번호 추천</p>
          <h3>추천 조합</h3>
          <span className="stat-link-cta">추천받기</span>
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
