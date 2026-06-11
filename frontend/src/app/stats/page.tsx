'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { PatternStatDto } from '@/lib/types'
import AdSlot from '@/components/AdSlot'

export default function StatsPage() {
  const [data, setData] = useState<PatternStatDto | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.stats.patterns().then(setData).catch(() => setData(null)).finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-slate-400 text-center py-12">불러오는 중…</p>

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">패턴 통계</p>
        <h1 className="text-2xl font-bold">패턴 통계</h1>
        <p className="text-slate-400 text-sm">통계 수치는 과거 분포이며, 다음 회차 예측 근거가 아닙니다.</p>
      </header>

      {data && (
        <>
          <section className="card space-y-3">
            <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">
              홀짝 분포 (총 {data.totalDraws.toLocaleString()}회)
            </h2>
            <div className="space-y-2">
              {data.oddEvenStats
                .slice()
                .sort((a, b) => b.hits - a.hits)
                .map((s) => (
                  <div key={`${s.oddCount}-${s.evenCount}`} className="flex items-center gap-3 text-sm">
                    <span className="w-16 text-slate-300 text-xs">홀 {s.oddCount} / 짝 {s.evenCount}</span>
                    <div className="flex-1">
                      <div className="h-2 rounded-full bg-gold/70" style={{ width: `${s.rate}%` }} />
                    </div>
                    <span className="font-mono text-xs text-slate-400 w-24 text-right">
                      {s.hits}회 ({s.rate.toFixed(1)}%)
                    </span>
                  </div>
                ))}
            </div>
          </section>

          <AdSlot slotId="stats-bottom" />

          <section className="card space-y-3">
            <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">합산 범위 분포</h2>
            <div className="space-y-2">
              {data.sumRangeStats
                .slice()
                .sort((a, b) => b.hits - a.hits)
                .map((s) => (
                  <div key={s.label} className="flex items-center gap-3 text-sm">
                    <span className="w-24 text-slate-300 text-xs">{s.label}</span>
                    <div className="flex-1">
                      <div className="h-2 rounded-full bg-blue-400/70" style={{ width: `${s.rate}%` }} />
                    </div>
                    <span className="font-mono text-xs text-slate-400 w-24 text-right">
                      {s.hits}회 ({s.rate.toFixed(1)}%)
                    </span>
                  </div>
                ))}
            </div>
          </section>
        </>
      )}
    </div>
  )
}
