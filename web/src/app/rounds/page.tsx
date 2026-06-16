import type { Metadata } from "next";
import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { JsonLdLottoRound } from "@/components/json-ld";
import { getLatestWinningNumber, getRounds, getPublicBaseUrl, type WinningNumber } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import { calcAfterTax } from "@/lib/tax";
import { RoundSearchForm } from "@/components/round-search-form";
import { headers } from "next/headers";
import logger from "@/lib/logger";
export const revalidate = 60;

export async function generateMetadata(): Promise<Metadata> {
  let latest: WinningNumber | null = null;
  try {
    latest = await getLatestWinningNumber();
  } catch {
    // no data yet
  }

  if (!latest) {
    return {
      title: "최신 결과 · 전체 회차 | KRAFT Lotto",
      description: "최신 로또 6/45 당첨 번호와 전체 회차 목록을 확인합니다.",
      alternates: { canonical: "/rounds" },
    };
  }

  return {
    title: `${latest.round}회 로또 당첨번호 · 전체 회차 | KRAFT Lotto`,
    description: `${latest.round}회 로또 6/45 당첨 번호 ${[...latest.numbers].join(", ")}(보너스 ${latest.bonusNumber}). 전체 회차 목록 조회.`,
    alternates: { canonical: "/rounds" },
    openGraph: {
      title: `${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      url: "/rounds",
    },
  };
}

function pageRange(current: number, total: number): (number | "…")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "…", total];
  if (current >= total - 3) return [1, "…", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "…", current - 1, current, current + 1, "…", total];
}

type Props = {
  searchParams: Promise<{ page?: string }>;
};

export default async function RoundsPage({ searchParams }: Props) {
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const { page: pageParam } = await searchParams;
  const userPage = Math.max(1, parseInt(pageParam ?? "1", 10) || 1);
  const apiPage = userPage - 1;

  const [latest, rounds] = await Promise.all([
    getLatestWinningNumber().catch((err) => {
      logger.warn({ err }, "최신 당첨번호 조회 실패");
      return null;
    }),
    getRounds(apiPage, 20),
  ]);

  const baseUrl = getPublicBaseUrl();
  const totalPages = rounds.totalPages;
  const currentPage = Math.min(userPage, totalPages || 1);
  const pages = pageRange(currentPage, totalPages);

  return (
    <>
      {latest && (
        <JsonLdLottoRound baseUrl={baseUrl} round={latest.round} drawDate={latest.drawDate} nonce={nonce} />
      )}

      {latest ? (
        <section className="panel" style={{ marginBottom: "24px" }}>
          <p className="eyebrow">최신 결과</p>
          <h1 className="page-title">{latest.round}회 당첨 결과</h1>
          <p className="page-subtitle">{formatDrawDate(latest.drawDate)}</p>
          <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
          <p className="muted">1등 당첨금 {formatCurrency(latest.firstPrizeAmount)}</p>
          <p className="muted">세후 예상 수령액 {formatCurrency(calcAfterTax(latest.firstPrizeAmount))}</p>
        </section>
      ) : (
        <section className="panel" style={{ marginBottom: "24px" }}>
          <p className="eyebrow">최신 결과</p>
          <h1 className="page-title">최신 결과를 준비 중입니다</h1>
          <p className="page-subtitle">잠시 후 다시 확인해 주세요.</p>
        </section>
      )}

      <section className="panel">
        <p className="eyebrow">회차 목록</p>
        <h2 className="page-title" style={{ fontSize: "clamp(1.3rem, 2vw, 1.8rem)" }}>전체 회차</h2>
        <RoundSearchForm />

        <p className="muted" style={{ marginTop: "20px" }}>
          총 {rounds.totalElements.toLocaleString()}회 ·&nbsp;
          {currentPage} / {totalPages} 페이지
        </p>

        <div className="round-list" style={{ marginTop: "24px" }}>
          {rounds.items.map((item) => (
            <article key={item.round} className="round-card">
              <div className="round-meta">
                <div>
                  <strong>{item.round}회</strong>
                  <p className="muted">{formatDrawDate(item.drawDate)}</p>
                </div>
                <Link href={`/rounds/${item.round}`} className="button secondary">
                  결과 상세
                </Link>
              </div>
              <LottoBalls numbers={item.numbers} bonusNumber={item.bonusNumber} />
              <p className="muted">1등 당첨금 {formatCurrency(item.firstPrizeAmount)}</p>
            </article>
          ))}
        </div>

        {totalPages > 1 && (
          <nav className="pagination" aria-label="페이지 이동">
            {currentPage > 1 ? (
              <Link href={`/rounds?page=${currentPage - 1}`} className="pagination-btn">
                ‹ 이전
              </Link>
            ) : (
              <span className="pagination-btn disabled">‹ 이전</span>
            )}

            {pages.map((p, i) =>
              p === "…" ? (
                <span key={`ellipsis-${i}`} className="pagination-ellipsis">…</span>
              ) : (
                <Link
                  key={p}
                  href={`/rounds?page=${p}`}
                  className={`pagination-btn${p === currentPage ? " active" : ""}`}
                >
                  {p}
                </Link>
              )
            )}

            {currentPage < totalPages ? (
              <Link href={`/rounds?page=${currentPage + 1}`} className="pagination-btn">
                다음 ›
              </Link>
            ) : (
              <span className="pagination-btn disabled">다음 ›</span>
            )}
          </nav>
        )}
      </section>
    </>
  );
}
