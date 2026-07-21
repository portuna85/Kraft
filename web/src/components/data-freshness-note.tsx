import type { RoundFreshness } from "@/lib/api";
import { formatDrawDate } from "@/lib/format";

export function DataFreshnessNote({ freshness }: { freshness: RoundFreshness | null }) {
  if (!freshness) return null;

  return (
    <p className="muted data-freshness-note" role="status">
      공식 데이터 기준 · {freshness.latestRound}회 ({formatDrawDate(freshness.latestDrawDate)})
      {freshness.fresh
        ? " · 최신 회차까지 반영됨"
        : " · 최신 추첨 결과 반영이 지연되고 있습니다. 잠시 후 다시 확인해 주세요."}
    </p>
  );
}
