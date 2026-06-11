'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { ServiceStatusDto } from '@/lib/types'

export default function StatusPage() {
  const [data, setData] = useState<ServiceStatusDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    api.service
      .status()
      .then(setData)
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">서비스</p>
        <h1 className="text-2xl font-bold">서비스 상태</h1>
        <p className="text-slate-400 text-sm">데이터 수집 현황 및 시스템 상태를 확인합니다.</p>
      </header>

      {loading && (
        <p className="text-slate-400 text-center py-12">불러오는 중…</p>
      )}

      {!loading && error && (
        <div className="card text-center py-8 text-red-400">
          상태 정보를 불러오지 못했습니다.
        </div>
      )}

      {!loading && data && (
        <>
          {/* 데이터 신선도 */}
          <section className="card space-y-4">
            <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">
              데이터 현황
            </h2>
            <div className="flex items-center gap-3">
              <span
                className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                  data.upToDate
                    ? 'bg-emerald-500/20 text-emerald-400'
                    : 'bg-amber-500/20 text-amber-400'
                }`}
              >
                <span
                  className={`w-1.5 h-1.5 rounded-full ${
                    data.upToDate ? 'bg-emerald-400' : 'bg-amber-400'
                  }`}
                />
                {data.upToDate ? '최신 상태' : `${data.roundsBehind}회차 미반영`}
              </span>
            </div>
            <dl className="grid grid-cols-2 gap-3 text-sm sm:grid-cols-3">
              <div className="space-y-0.5">
                <dt className="text-slate-500 text-xs">저장된 최신 회차</dt>
                <dd className="font-mono font-semibold">
                  {data.latestRound != null ? `${data.latestRound}회` : '—'}
                </dd>
              </div>
              <div className="space-y-0.5">
                <dt className="text-slate-500 text-xs">추첨일</dt>
                <dd className="font-mono text-slate-300">
                  {data.latestDrawDate ?? '—'}
                </dd>
              </div>
              <div className="space-y-0.5">
                <dt className="text-slate-500 text-xs">현재 예상 최신 회차</dt>
                <dd className="font-mono text-slate-300">{data.expectedRound}회</dd>
              </div>
            </dl>
          </section>

          {/* 최근 수집 이력 */}
          {data.recentLogs.length > 0 && (
            <section className="card space-y-3">
              <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">
                최근 수집 이력 (최근 20건)
              </h2>
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="text-slate-500 border-b border-[#0F3460]">
                      <th className="text-left pb-2 pr-4">회차</th>
                      <th className="text-left pb-2 pr-4">상태</th>
                      <th className="text-left pb-2">수집 시각</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#0F3460]/50">
                    {data.recentLogs.map((log) => (
                      <tr key={`${log.round}-${log.fetchedAtFormatted}`}>
                        <td className="py-1.5 pr-4 font-mono">{log.round}회</td>
                        <td className="py-1.5 pr-4">
                          <StatusBadge cssClass={log.statusCssClass} label={log.statusLabel} />
                        </td>
                        <td className="py-1.5 font-mono text-slate-400">
                          {log.fetchedAtFormatted}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {/* 빌드 정보 */}
          {(data.appVersion || data.buildTime) && (
            <section className="card space-y-3">
              <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">
                빌드 정보
              </h2>
              <dl className="text-sm space-y-1">
                {data.appVersion && (
                  <div className="flex gap-3">
                    <dt className="text-slate-500 w-24 shrink-0">버전</dt>
                    <dd className="font-mono text-slate-300">v{data.appVersion}</dd>
                  </div>
                )}
                {data.buildTime && (
                  <div className="flex gap-3">
                    <dt className="text-slate-500 w-24 shrink-0">빌드 시각</dt>
                    <dd className="font-mono text-slate-300">{data.buildTime}</dd>
                  </div>
                )}
              </dl>
            </section>
          )}
        </>
      )}
    </div>
  )
}

function StatusBadge({ cssClass, label }: { cssClass: string; label: string }) {
  const color =
    cssClass === 'changelog-success'
      ? 'text-emerald-400'
      : cssClass === 'changelog-failed'
      ? 'text-red-400'
      : cssClass === 'changelog-skipped'
      ? 'text-slate-400'
      : 'text-slate-500'

  return <span className={color}>{label}</span>
}
