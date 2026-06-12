'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { NumberFrequencyDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'

const PERIODS = [
  { label: '전체', value: undefined },
  { label: '최근 100회', value: 100 },
  { label: '최근 200회', value: 200 },
  { label: '최근 500회', value: 500 },
]

export default function FrequencyPage() {
  const [period, setPeriod] = useState<number | undefined>(undefined)
  const [data, setData] = useState<NumberFrequencyDto[] | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    api.stats.frequency(period)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [period])

  return (
    <div data-testid="frequency-page" className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">출현 빈도</p>
        <h1 className="text-2xl font-bold">번호별 출현 빈도</h1>
        <p className="text-slate-400 text-sm">출현 빈도는 과거 데이터입니다. 당첨 확률을 높이지 않습니다.</p>
      </header>

      <div className="flex gap-2 flex-wrap">
        {PERIODS.map(({ label, value }) => (
          <button
            key={label}
            onClick={() => setPeriod(value)}
            className={`px-3 py-1 rounded text-sm transition-colors ${
              period === value ? 'bg-gold text-navy font-bold' : 'bg-[#16213E] text-slate-400 hover:text-white'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <AdSlot slotId="frequency-bottom" />

      {loading && <p className="text-slate-400 text-center py-8">불러오는 중…</p>}
      {data && (
        <div className="card">
          <div className="flex flex-wrap gap-4 justify-start">
            {data
              .slice()
              .sort((a, b) => b.count - a.count)
              .map((d) => (
                <div key={d.number} className="flex flex-col items-center gap-1">
                  <LottoBall number={d.number} size="sm" />
                  <span className="text-xs text-slate-400 font-mono leading-tight">{d.count}회</span>
                  <span className="text-xs text-slate-500 font-mono leading-tight">{d.rate.toFixed(1)}%</span>
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  )
}
