import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { getLatestWinningNumber } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";

export const dynamic = "force-dynamic";

export default async function HomePage() {
  const latest = await getLatestWinningNumber();

  return (
    <div className="grid">
      <section className="hero">
        <div>
          <div className="eyebrow">KST / 한국어 기준 서비스</div>
          <h1>최신 로또 회차와 저장한 번호를 한 화면에서 관리합니다.</h1>
          <p>
            현재 저장소의 백엔드 API와 직접 연결된 SSR 웹 앱입니다.
            최신 회차, 추천 번호, 저장함을 공개 도메인에서 바로 제공합니다.
          </p>
          <div className="hero-actions">
            <Link href="/latest" className="button">최신 회차 보기</Link>
            <Link href="/recommend" className="button secondary">번호 추천 받기</Link>
            <Link href="/saved" className="button secondary">저장함 열기</Link>
          </div>
        </div>
        <aside className="hero-side">
          <p className="eyebrow">최근 추첨</p>
          <h2>{latest.round}회</h2>
          <p className="muted">{formatDrawDate(latest.drawDate)}</p>
          <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
          <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
        </aside>
      </section>

      <section className="grid grid-3">
        <article className="stat-card">
          <p className="eyebrow">조회</p>
          <h3>최신 회차 / 과거 회차</h3>
          <p className="muted">SSR로 최신 회차를 즉시 렌더링하고, 회차 목록 페이지에서 과거 데이터까지 이어서 탐색합니다.</p>
        </article>
        <article className="stat-card">
          <p className="eyebrow">추천</p>
          <h3>API 기반 번호 추천</h3>
          <p className="muted">백엔드 추천 API를 기준으로 동작하며, 추천 결과를 저장함으로 바로 넘길 수 있게 연결했습니다.</p>
        </article>
        <article className="stat-card">
          <p className="eyebrow">저장</p>
          <h3>기기 토큰 해시 저장</h3>
          <p className="muted">저장함은 브라우저 토큰을 직접 보내되 서버에는 SHA-256 해시만 남기도록 연결했습니다.</p>
        </article>
      </section>
    </div>
  );
}
