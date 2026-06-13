import type { Metadata } from "next";
import Link from "next/link";
import { LottoBalls } from "@/components/lotto-balls";
import { getRounds } from "@/lib/api";
import { formatCurrency, formatDrawDate } from "@/lib/format";

export const dynamic = "force-dynamic";

export const metadata: Metadata = {
  title: "전체 회차 목록 | KRAFT Lotto",
  description: "로또 6/45 제1회부터 최신 회차까지 전체 당첨번호 목록입니다.",
  alternates: { canonical: "/rounds" },
};

type Props = {
  searchParams: Promise<{ page?: string }>;
};

function pageRange(current: number, total: number): (number | "…")[] {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "…", total];
  if (current >= total - 3) return [1, "…", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "…", current - 1, current, current + 1, "…", total];
}

export default async function RoundsPage({ searchParams }: Props) {
  const { page: pageParam } = await searchParams;
  // URL uses 1-indexed pages; API uses 0-indexed
  const userPage = Math.max(1, parseInt(pageParam ?? "1", 10) || 1);
  const apiPage = userPage - 1;

  const rounds = await getRounds(apiPage, 20);
  const totalPages = rounds.totalPages;
  const currentPage = Math.min(userPage, totalPages || 1);

  const pages = pageRange(currentPage, totalPages);

  return (
    <section className="panel">
      <p className="eyebrow">회차 목록</p>
      <h1 className="page-title">전체 회차</h1>
      <p className="muted">
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
                상세 보기
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
  );
}
