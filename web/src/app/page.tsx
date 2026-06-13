import type { Metadata } from "next";
import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { getLatestWinningNumber, WinningNumber } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import logger from "@/lib/logger";

export const dynamic = "force-dynamic";

export async function generateMetadata(): Promise<Metadata> {
  try {
    const latest = await getLatestWinningNumber();
    return {
      title: `제${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      description: `제${latest.round}회 로또 6/45 당첨번호를 KST 기준으로 확인하세요. 번호 추천, 빈도 통계, 저장함 기능을 무료로 제공합니다.`,
      alternates: { canonical: "/" },
      openGraph: {
        title: `제${latest.round}회 로또 당첨번호 | KRAFT Lotto`,
        description: `제${latest.round}회 당첨번호 ${[...latest.numbers, latest.bonusNumber].join(", ")}`,
        url: "/",
      },
    };
  } catch {
    return {
      title: "KRAFT Lotto | 한국 로또 번호 조회",
      description: "매주 토요일 최신 로또 회차를 KST 기준으로 자동 업데이트합니다. 번호 추천, 빈도 통계, 저장함 기능을 무료로 제공합니다.",
      alternates: { canonical: "/" },
    };
  }
}

export default async function HomePage() {
  let latest: WinningNumber | null = null;
  try {
    latest = await getLatestWinningNumber();
  } catch (err) {
    logger.warn({ err }, "최신 당첨번호 조회 실패 (데이터 없음)");
  }

  return (
    <div className="grid">
      <section className="hero">
        <div>
          <div className="eyebrow">KST 기준 · 매주 토요일 자동 업데이트</div>
          <h1>로또 6/45 당첨번호 조회와 번호 추천을 한 번에</h1>
          <p>
            최신 회차부터 제1회까지 전체 당첨번호를 확인하고,
            통계 기반 분석과 무작위 번호 추천을 무료로 이용하세요.
          </p>
          <div className="hero-actions">
            <Link href="/latest" className="button">최신 회차 보기</Link>
            <Link href="/recommend" className="button secondary">번호 추천 받기</Link>
            <Link href="/saved" className="button secondary">저장함 열기</Link>
          </div>
        </div>
        <aside className="hero-side">
          {latest ? (
            <>
              <p className="eyebrow">최근 추첨</p>
              <h2>{latest.round}회</h2>
              <p className="muted">{formatDrawDate(latest.drawDate)}</p>
              <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
              <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
            </>
          ) : (
            <>
              <p className="eyebrow">최근 추첨</p>
              <p className="muted">아직 당첨번호 데이터가 없습니다.</p>
              <Link href="/rounds" className="button secondary">회차 목록 보기</Link>
            </>
          )}
        </aside>
      </section>

      <section className="grid grid-3">
        <article className="stat-card">
          <p className="eyebrow">조회</p>
          <h3>최신 · 전체 회차</h3>
          <p className="muted">제1회부터 최신 회차까지 모든 당첨번호를 조회하고, 회차별 상세 정보와 당첨금을 확인하세요.</p>
        </article>
        <article className="stat-card">
          <p className="eyebrow">추천</p>
          <h3>무작위 번호 추천</h3>
          <p className="muted">제외할 번호를 지정해 최대 10세트까지 추천받고, 마음에 드는 번호를 저장함으로 바로 보낼 수 있습니다.</p>
        </article>
        <article className="stat-card">
          <p className="eyebrow">통계</p>
          <h3>빈도 · 패턴 · 동반 분석</h3>
          <p className="muted">번호별 출현 빈도, 홀짝 분포, 자주 함께 나오는 번호 쌍 등 전체 회차 통계를 무료로 열람하세요.</p>
        </article>
      </section>
    </div>
  );
}
