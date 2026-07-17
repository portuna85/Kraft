import type { Metadata } from "next";
import { getPublicIncidents, getRoundFreshness } from "@/lib/api";
import { formatDateTime, formatDrawDate } from "@/lib/format";
import logger from "@/lib/logger";

// 루트 레이아웃이 CSP nonce를 위해 headers()를 호출해 전 페이지가 동적 렌더링되므로
// 이 값은 Full Route Cache에 영향을 주지 않는다(문서화 목적으로 유지, 실제 캐시는 lib/api.ts의 fetch revalidate가 담당).
export const revalidate = 60;

export const metadata: Metadata = {
  title: "서비스 상태",
  description: "데이터 최신성과 최근 수집·보정 이력을 안내합니다.",
  robots: { index: false, follow: false },
  alternates: { canonical: "/status" },
};

export default async function StatusPage() {
  const [freshness, incidents] = await Promise.all([
    getRoundFreshness().catch((error) => {
      logger.warn({ err: error }, "데이터 최신성 조회 실패");
      return null;
    }),
    getPublicIncidents().catch((error) => {
      logger.warn({ err: error }, "공개 이력 조회 실패");
      return null;
    }),
  ]);

  return (
    <section className="panel">
      <p className="eyebrow">서비스 상태</p>
      <h1 className="page-title">서비스 상태</h1>

      <h2 className="section-title">현재 데이터 상태</h2>
      {freshness ? (
        <p className="muted">
          {freshness.latestRound}회 ({formatDrawDate(freshness.latestDrawDate)}) 까지 반영됨 ·{" "}
          {freshness.fresh ? "정상" : "최신 회차 반영 지연 중"}
        </p>
      ) : (
        <p className="muted">상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.</p>
      )}

      <h2 className="section-title">최근 30일 수집·보정 이력</h2>
      {!incidents ? (
        <p className="muted">이력을 불러오지 못했습니다.</p>
      ) : incidents.length === 0 ? (
        <p className="muted">최근 30일 동안 기록된 수집 지연이나 보정 이력이 없습니다.</p>
      ) : (
        <ul className="status-incident-list">
          {incidents.map((incident) => (
            <li
              key={`${incident.type}-${incident.round}`}
              className="status-incident-item"
            >
              <span className="status-incident-round">{incident.round ? `${incident.round}회` : "-"}</span>
              <span className="status-incident-type">
                {incident.type}
                {incident.occurrences > 1 ? ` (시도 ${incident.occurrences}회)` : ""}
              </span>
              <span className={`status-incident-state${incident.resolved ? " resolved" : ""}`}>
                {incident.resolved ? "해결됨" : "확인 중"}
              </span>
              <span className="status-incident-time muted">{formatDateTime(incident.occurredAt)}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
