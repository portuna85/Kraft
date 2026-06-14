import type { Metadata } from "next";
import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { getLatestWinningNumber, WinningNumber } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import logger from "@/lib/logger";

export const revalidate = 60;

export async function generateMetadata(): Promise<Metadata> {
  try {
    const latest = await getLatestWinningNumber();
    return {
      title: `제${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      description: `제${latest.round}회 로또 6/45 당첨 결과를 확인하고 추천 번호와 통계 기능까지 함께 이용하세요.`,
      alternates: { canonical: "/" },
      openGraph: {
        title: `제${latest.round}회 로또 당첨번호 | KRAFT Lotto`,
        description: `제${latest.round}회 당첨 번호 ${[...latest.numbers, latest.bonusNumber].join(", ")}`,
        url: "/",
      },
    };
  } catch {
    return {
      title: "KRAFT Lotto | 로또 당첨 결과와 번호 관리",
      description: "최신 회차부터 전체 기록까지 확인하고, 번호 추천과 저장 기능까지 한 번에 이용하세요.",
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
          <div className="eyebrow">공식 발표 기준 · KST 반영</div>
          <h1>최신 로또 당첨 결과와 내 번호 관리를 한 곳에서</h1>
          <p>
            최신 회차 확인, 과거 기록 탐색, 추천 번호 생성, 저장한 조합 관리까지
            실제 구매 전에 필요한 흐름을 간결하게 정리했습니다.
          </p>
          <div className="hero-actions">
            <Link href="/latest" className="button">최신 결과 확인</Link>
            <Link href="/recommend" className="button secondary">추천 번호 만들기</Link>
            <Link href="/saved" className="button secondary">내 번호 모아보기</Link>
          </div>
        </div>
        <aside className="hero-side">
          {latest ? (
            <>
              <p className="eyebrow">가장 최근 추첨</p>
              <h2>{latest.round}회</h2>
              <p className="muted">{formatDrawDate(latest.drawDate)}</p>
              <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
              <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
            </>
          ) : (
            <>
              <p className="eyebrow">가장 최근 추첨</p>
              <p className="muted">표시할 당첨 결과가 아직 준비되지 않았습니다.</p>
              <Link href="/rounds" className="button secondary">전체 회차 둘러보기</Link>
            </>
          )}
        </aside>
      </section>

      <section className="grid grid-3">
        <article className="stat-card">
          <p className="eyebrow">조회</p>
          <h3>최신 결과부터 과거 기록까지</h3>
          <p className="muted">최신 회차는 바로 확인하고, 필요한 경우 과거 회차 상세와 당첨 금액까지 이어서 살펴볼 수 있습니다.</p>
        </article>
        <article className="stat-card">
          <p className="eyebrow">추천</p>
          <h3>조건을 반영한 추천 번호</h3>
          <p className="muted">제외하고 싶은 숫자를 반영해 여러 조합을 만들고, 괜찮은 조합은 바로 저장해 다시 꺼내볼 수 있습니다.</p>
        </article>
        <article className="stat-card">
          <p className="eyebrow">통계</p>
          <h3>빈도와 패턴을 빠르게 확인</h3>
          <p className="muted">출현 빈도, 번호 분포, 함께 나온 번호 조합을 한눈에 보고 참고용 통계로 활용할 수 있습니다.</p>
        </article>
      </section>
    </div>
  );
}
