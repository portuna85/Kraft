import type { Metadata } from "next";
import Link from "next/link";
import { headers } from "next/headers";
import { LottoBalls } from "@/components/lotto-balls";
import { PrizeTable } from "@/components/prize-table";
import { RoundSearchForm } from "@/components/round-search-form";
import { DataFreshnessNote } from "@/components/data-freshness-note";
import { JsonLdLottoRound } from "@/components/json-ld";
import { getLatestWinningNumber, getPublicBaseUrl, getRounds, type WinningNumber } from "@/lib/api";
import { PageAd } from "@/components/ad-unit";
import { formatCurrency, formatDrawDate } from "@/lib/format";
import logger from "@/lib/logger";

export const revalidate = 60;

type PageItem = number | "ellipsis";

export async function generateMetadata(): Promise<Metadata> {
  let latest: WinningNumber | null = null;

  try {
    latest = await getLatestWinningNumber();
  } catch {
    // ignore metadata fallback
  }

  if (!latest) {
    return {
      title: "최신 결과와 전체 회차",
      description: "최신 로또 6/45 당첨 번호와 전체 회차 목록을 확인할 수 있습니다.",
      alternates: { canonical: "/rounds" },
    };
  }

  return {
    title: `${latest.round}회 로또 당첨번호와 전체 회차`,
    description: `${latest.round}회 로또 6/45 당첨 번호 ${[...latest.numbers].join(", ")} 보너스 ${latest.bonusNumber}. 전체 회차 목록을 함께 제공합니다.`,
    alternates: { canonical: "/rounds" },
    openGraph: {
      title: `${latest.round}회 로또 당첨번호 (${latest.drawDate}) | KRAFT Lotto`,
      url: "/rounds",
    },
  };
}

function pageRange(current: number, total: number): PageItem[] {
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
  if (current >= total - 3) return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
}

type Props = {
  searchParams: Promise<{ page?: string }>;
};

export default async function RoundsPage({ searchParams }: Props) {
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const { page: pageParam } = await searchParams;
  const userPage = Math.max(1, Number.parseInt(pageParam ?? "1", 10) || 1);
  const apiPage = userPage - 1;

  const [latest, rounds] = await Promise.all([
    getLatestWinningNumber().catch((error) => {
      logger.warn({ err: error }, "최신 당첨번호 조회 실패");
      return null;
    }),
    getRounds(apiPage, 20),
  ]);

  const baseUrl = getPublicBaseUrl();
  const totalPages = rounds.totalPages;
  const currentPage = Math.min(userPage, totalPages || 1);
  const pages = pageRange(currentPage, totalPages);

  return (
    <div className="section-stack">
      {latest && (
        <JsonLdLottoRound
          baseUrl={baseUrl}
          round={latest.round}
          drawDate={latest.drawDate}
          nonce={nonce}
          pageUrl={`${baseUrl}/rounds`}
        />
      )}

      {latest ? (
        <section className="panel result-panel hero-panel">
          <p className="eyebrow">최신 결과</p>
          <h1 className="result-title">
            {latest.round}회 당첨 결과 <span className="result-date">({formatDrawDate(latest.drawDate)})</span>
          </h1>
          <LottoBalls numbers={latest.numbers} bonusNumber={latest.bonusNumber} />
          <PrizeTable firstPrizeAmount={latest.firstPrizeAmount} secondPrize={latest.secondPrize} />
          <DataFreshnessNote />
        </section>
      ) : (
        <section className="panel result-panel hero-panel">
          <p className="eyebrow">최신 결과</p>
          <h1 className="result-title">최신 결과를 준비 중입니다</h1>
          <p className="page-subtitle">잠시 후 다시 확인해 주세요.</p>
        </section>
      )}

      <section className="panel">
        <p className="eyebrow">회차 목록</p>
        <h2 className="page-title rounds-title">전체 회차</h2>
        <RoundSearchForm />

        <p className="muted rounds-summary">
          총 {rounds.totalElements.toLocaleString()}회 중 {currentPage} / {totalPages} 페이지
        </p>

        <div className="round-list round-list-wrap">
          {rounds.items.map((item) => (
            <article key={item.round} className="round-card">
              <div className="round-meta">
                <div>
                  <strong>{item.round}회</strong>
                  <p className="muted">{formatDrawDate(item.drawDate)}</p>
                </div>
                <Link href={`/rounds/${item.round}`} className="button secondary round-card-link">
                  결과 상세
                </Link>
              </div>
              <LottoBalls numbers={item.numbers} bonusNumber={item.bonusNumber} />
              <p className="muted prize-line">1등 당첨금 {formatCurrency(item.firstPrizeAmount)}</p>
            </article>
          ))}
        </div>

        {totalPages > 1 && (
          <nav className="pagination" aria-label="페이지 이동">
            {currentPage > 1 ? (
              <Link href={`/rounds?page=${currentPage - 1}`} className="pagination-btn">
                이전
              </Link>
            ) : (
              <span className="pagination-btn disabled">이전</span>
            )}

            {pages.map((page, index) =>
              page === "ellipsis" ? (
                <span key={`ellipsis-${index}`} className="pagination-ellipsis">
                  ...
                </span>
              ) : (
                <Link
                  key={page}
                  href={`/rounds?page=${page}`}
                  className={`pagination-btn${page === currentPage ? " active" : ""}`}
                >
                  {page}
                </Link>
              ),
            )}

            {currentPage < totalPages ? (
              <Link href={`/rounds?page=${currentPage + 1}`} className="pagination-btn">
                다음
              </Link>
            ) : (
              <span className="pagination-btn disabled">다음</span>
            )}
          </nav>
        )}
      </section>

      <PageAd slot="rounds-list" />
    </div>
  );
}
