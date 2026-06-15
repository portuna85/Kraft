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
      description: `${latest.round}회 최신 결과와 번호 추천, 저장 기능을 한 곳에서 확인할 수 있습니다.`,
      alternates: { canonical: "/" },
      openGraph: {
        title: `${latest.round}회 로또 당첨번호 | KRAFT Lotto`,
        description: `${latest.round}회 당첨 번호 ${[...latest.numbers, latest.bonusNumber].join(", ")}`,
        url: "/",
      },
    };
  } catch {
    return {
      title: "KRAFT Lotto | 로또 결과와 번호 관리",
      description: "최신 결과 확인, 회차 조회, 번호 추천, 저장 기능을 한 곳에서 제공합니다.",
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
          <div className="eyebrow">공식 발표 기준 · KST 반영</div>
          <h1>로또 결과와 번호 관리를 한 곳에서</h1>
          <p>최신 결과 확인, 번호 추천, 저장까지 필요한 기능만 간단하게 모았습니다.</p>
          <div className="hero-actions">
            <Link href="/latest" className="button">최신 결과 보기</Link>
            <Link href="/recommend" className="button secondary">추천 번호 만들기</Link>
            <Link href="/saved" className="button secondary">저장한 번호 보기</Link>
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
          <h3>최신 결과 보기</h3>
          <p className="muted">가장 최근 당첨 번호와 당첨금을 바로 확인합니다.</p>
          <span className="stat-link-cta">바로 보기</span>
        </Link>

        <Link href="/recommend" className="stat-card stat-link">
          <p className="eyebrow">번호 추천</p>
          <h3>추천 조합 만들기</h3>
          <p className="muted">제외 번호를 반영해 조합을 만들고 저장할 수 있습니다.</p>
          <span className="stat-link-cta">추천받기</span>
        </Link>

        <Link href="/frequency" className="stat-card stat-link">
          <p className="eyebrow">통계 참고</p>
          <h3>출현 통계 보기</h3>
          <p className="muted">자주 나온 번호와 분포를 빠르게 비교합니다.</p>
          <span className="stat-link-cta">통계 보기</span>
        </Link>
      </section>
    </div>
  );
}
