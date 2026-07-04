import type { Metadata } from "next";
import Link from "next/link";
import { headers } from "next/headers";
import { notFound } from "next/navigation";
import { LottoBalls } from "@/components/lotto-balls";
import { PrizeTable } from "@/components/prize-table";
import { JsonLdLottoRound } from "@/components/json-ld";
import { getLatestWinningNumber, getPublicBaseUrl, getRound, type AnalysisResponse } from "@/lib/api";
import { analyzeNumbers } from "@/lib/analyze";
import { PageAd } from "@/components/ad-unit";
import { formatCurrency, formatDrawDate } from "@/lib/format";

// 루트 레이아웃이 CSP nonce를 위해 headers()를 호출해 전 페이지가 동적 렌더링되므로
// 이 값은 Full Route Cache에 영향을 주지 않는다(문서화 목적으로 유지, 실제 캐시는 lib/api.ts의 fetch revalidate가 담당).
export const revalidate = 3600;

type Props = {
  params: Promise<{ round: string }>;
};

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { round } = await params;
  const roundNumber = Number(round);

  if (Number.isNaN(roundNumber) || roundNumber < 1) return {};

  try {
    const data = await getRound(roundNumber);
    const balls = [...data.numbers, data.bonusNumber].join(", ");
    const baseUrl = getPublicBaseUrl();

    return {
      title: `${data.round}회 로또 당첨번호 (${data.drawDate})`,
      description: `${data.round}회 로또 6/45 당첨 번호 ${balls}. 1등 당첨금 ${formatCurrency(data.firstPrizeAmount)}.`,
      alternates: { canonical: `/rounds/${data.round}` },
      openGraph: {
        title: `${data.round}회 로또 당첨번호`,
        description: `당첨번호 ${data.numbers.join(" ")} + ${data.bonusNumber} | ${formatDrawDate(data.drawDate)}`,
        url: `/rounds/${data.round}`,
        images: [{ url: `${baseUrl}/api/og/round/${data.round}?b=${data.numbers.join(",")}&bo=${data.bonusNumber}&d=${data.drawDate}&p=${data.firstPrizeAmount}`, width: 1200, height: 630 }],
      },
    };
  } catch {
    return {};
  }
}

export default async function RoundDetailPage({ params }: Props) {
  const { round } = await params;
  const roundNumber = Number(round);

  if (Number.isNaN(roundNumber) || roundNumber < 1) {
    notFound();
  }

  let latestRound = 0;

  try {
    const latest = await getLatestWinningNumber();
    latestRound = latest.round;
  } catch {
    // ignore latest round fallback
  }

  const data = await getRound(roundNumber).catch(() => null);
  if (!data) notFound();

  const analysis = analyzeNumbers(data.numbers);

  const hasPrev = data.round > 1;
  const hasNext = latestRound > 0 ? data.round < latestRound : false;
  const nonce = (await headers()).get("x-nonce") ?? undefined;
  const baseUrl = getPublicBaseUrl();

  return (
    <section className="panel">
      <JsonLdLottoRound
        baseUrl={baseUrl}
        round={data.round}
        drawDate={data.drawDate}
        nonce={nonce}
        analysis={analysis}
      />
      <p className="eyebrow">회차 상세</p>
      <h1 className="page-title">{data.round}회 당첨 결과</h1>
      <p className="page-subtitle">{formatDrawDate(data.drawDate)}</p>

      <div className="round-detail-balls">
        <LottoBalls numbers={data.numbers} bonusNumber={data.bonusNumber} />
      </div>

      <PrizeTable firstPrizeAmount={data.firstPrizeAmount} secondPrize={data.secondPrize} />

      <div className="round-detail-grid">
        <div className="round-detail-cell">
          <p className="round-detail-label">총 판매 금액</p>
          <p className="round-detail-value">{formatCurrency(data.totalSales)}</p>
        </div>
      </div>

      <RoundAnalysisSection analysis={analysis} />

      <PageAd slot="rounds-detail" />

      <nav className="round-nav" aria-label="회차 이동">
        {hasPrev ? (
          <Link href={`/rounds/${data.round - 1}`} className="button secondary">
            {data.round - 1}회
          </Link>
        ) : (
          <span className="round-nav-slot" aria-hidden="true" />
        )}

        <Link href="/rounds" className="button secondary">
          전체 목록
        </Link>

        {hasNext ? (
          <Link href={`/rounds/${data.round + 1}`} className="button secondary">
            {data.round + 1}회
          </Link>
        ) : (
          <span className="round-nav-slot" aria-hidden="true" />
        )}
      </nav>
    </section>
  );
}

function RoundAnalysisSection({ analysis }: { analysis: AnalysisResponse }) {
  return (
    <div className="analysis-result">
      <h2 className="section-title">당첨 번호 분석</h2>

      <div className="result-grid">
        <div className="result-cell">
          <span className="result-label">홀수 / 짝수</span>
          <span className="result-value">{analysis.oddCount} / {analysis.evenCount}</span>
        </div>
        <div className="result-cell">
          <span className="result-label">저번호 / 고번호</span>
          <span className="result-value">{analysis.lowCount} / {analysis.highCount}</span>
        </div>
        <div className="result-cell">
          <span className="result-label">합계</span>
          <span className="result-value">{analysis.sumOfNumbers}</span>
          <span className="result-sub">{analysis.sumBucket} 구간</span>
        </div>
        <div className="result-cell">
          <span className="result-label">연속 번호</span>
          <span className="result-value">{analysis.consecutivePairCount}쌍</span>
        </div>
      </div>

      <div>
        <p className="section-title analysis-section-title">구간 분포</p>
        <ul className="range-dist-list">
          {analysis.rangeDistribution.map((range) => (
            <li key={range.range} className="range-dist-item">
              <span className="range-label">{range.range}</span>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{ width: `${Math.round((range.count / 6) * 100)}%` }}
                />
              </div>
              <span className="range-count">{range.count}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
