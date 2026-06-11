'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { ServiceStatusDto } from '@/lib/types'

export default function DataSourcePage() {
  const [data, setData] = useState<ServiceStatusDto | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.service
      .status()
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="space-y-4 text-sm text-slate-300">
      <div className="space-y-2">
        <h2 className="font-semibold text-white">당첨번호 데이터</h2>
        <dl className="space-y-1.5">
          <Row label="출처">
            <a
              href="https://www.dhlottery.co.kr"
              target="_blank"
              rel="noopener noreferrer"
              className="text-gold underline"
            >
              동행복권 공식 사이트
            </a>에서 제공하는 공개 API
          </Row>
          <Row label="수집 방식">동행복권 공개 API를 통해 자동 수집</Row>
          <Row label="갱신 주기">토요일 추첨 후 당일 22시 30분, 일요일 07시 00분 자동 수집</Row>
          <Row label="보관 범위">1회부터 최신 회차까지 전체</Row>
          <Row label="오류 발생 시">
            수집 실패 시 로그를 기록하고 다음 스케줄에 재시도합니다.
          </Row>
        </dl>
      </div>

      {loading && <p className="text-slate-500">상태 정보 불러오는 중…</p>}

      {!loading && data && (
        <>
          <div className="space-y-2 pt-3 border-t border-[#0F3460]">
            <h2 className="font-semibold text-white">현재 데이터 상태</h2>
            <dl className="space-y-1.5">
              <Row label="저장된 최신 회차">
                <span>
                  {data.latestRound != null
                    ? `${data.latestRound}회 (${data.latestDrawDate})`
                    : '—'}
                </span>
                {data.upToDate ? (
                  <span className="ml-2 text-emerald-400 text-xs">✓ 최신 상태</span>
                ) : (
                  <span className="ml-2 text-amber-400 text-xs">미반영 회차 있음</span>
                )}
              </Row>
              <Row label="현재 예상 최신 회차">{data.expectedRound}회</Row>
            </dl>
          </div>

          {data.recentLogs.length > 0 && (
            <div className="space-y-2 pt-3 border-t border-[#0F3460]">
              <h2 className="font-semibold text-white">최근 수집 이력</h2>
              <p className="text-slate-500 text-xs">최근 20건의 데이터 수집 결과입니다.</p>
              <div className="overflow-x-auto">
                <table className="w-full text-xs">
                  <thead>
                    <tr className="text-slate-500 border-b border-[#0F3460]">
                      <th className="text-left pb-1.5 pr-4">회차</th>
                      <th className="text-left pb-1.5 pr-4">상태</th>
                      <th className="text-left pb-1.5">수집 시각</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#0F3460]/50">
                    {data.recentLogs.map((log) => (
                      <tr key={`${log.round}-${log.fetchedAtFormatted}`}>
                        <td className="py-1 pr-4 font-mono">{log.round}회</td>
                        <td className="py-1 pr-4">
                          <LogBadge cssClass={log.statusCssClass} label={log.statusLabel} />
                        </td>
                        <td className="py-1 font-mono text-slate-400">{log.fetchedAtFormatted}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {(data.appVersion || data.buildTime) && (
            <div className="space-y-2 pt-3 border-t border-[#0F3460]">
              <h2 className="font-semibold text-white">Runtime</h2>
              <dl className="space-y-1.5">
                {data.appVersion && <Row label="버전">v{data.appVersion}</Row>}
                {data.buildTime && <Row label="빌드 시각">{data.buildTime}</Row>}
              </dl>
            </div>
          )}
        </>
      )}

      <p className="text-slate-500 text-xs pt-3 border-t border-[#0F3460]">
        데이터 불일치나 오류가 있는 경우 항상{' '}
        <a
          href="https://www.dhlottery.co.kr"
          target="_blank"
          rel="noopener noreferrer"
          className="text-gold underline"
        >
          동행복권 공식 사이트
        </a>의 발표를 우선합니다.{' '}
        오류 발견 시 <a href="/info/contact" className="text-gold underline">문의</a>해 주세요.
      </p>
    </div>
  )
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-2">
      <dt className="font-semibold text-slate-400 shrink-0 w-28">{label}</dt>
      <dd className="flex-1">{children}</dd>
    </div>
  )
}

function LogBadge({ cssClass, label }: { cssClass: string; label: string }) {
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
