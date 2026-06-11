'use client'

import { useState } from 'react'
import { api } from '@/lib/api'
import type { CompanionNumberDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'

export default function CompanionPage() {
  const [target, setTarget] = useState<number | null>(null)
  const [data, setData] = useState<CompanionNumberDto[] | null>(null)
  const [loading, setLoading] = useState(false)

  async function select(n: number) {
    if (target === n) { setTarget(null); setData(null); return }
    setTarget(n)
    setLoading(true)
    try {
      setData(await api.stats.companion(n))
    } catch {
      setData(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div data-testid="companion-page" className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">동반 출현 기록</p>
        <h1 className="text-2xl font-bold">동반 출현 기록</h1>
        <p className="text-slate-400 text-sm">번호 하나를 선택하면 함께 자주 나온 번호를 확인합니다.</p>
      </header>

      <AdSlot slotId="companion-top" />

      <div className="card space-y-4">
        <h2 className="text-sm text-slate-400">기준 번호 선택</h2>
        <div className="grid grid-cols-9 gap-1">
          {Array.from({ length: 45 }, (_, i) => i + 1).map((n) => {
            const c = ballBg(n)
            return (
              <button
                key={n}
                onClick={() => select(n)}
                className={`aspect-square rounded-full text-xs font-bold transition-all ${
                  target === n ? 'ring-2 ring-gold scale-110' : 'hover:scale-105'
                }`}
                style={{ backgroundColor: c.bg, color: c.text }}
              >
                {n}
              </button>
            )
          })}
        </div>
      </div>

      {loading && <p className="text-slate-400 text-center py-4">불러오는 중…</p>}

      {data && target && (
        <section className="card space-y-4">
          <h2 className="font-semibold">
            <LottoBall number={target} size="sm" /> 번과 함께 나온 번호 (상위 10)
          </h2>
          <div className="flex flex-wrap gap-3">
            {data.slice(0, 10).map((d) => (
              <div key={d.number} className="flex flex-col items-center gap-1">
                <LottoBall number={d.number} />
                <span className="text-xs text-slate-400">{d.count}회</span>
                <span className="text-xs text-slate-400">{d.percent.toFixed(1)}%</span>
              </div>
            ))}
          </div>
          <ul className="divide-y divide-[#0F3460] text-sm">
            {data.map((d) => (
              <li key={d.number} className="flex items-center gap-3 py-2">
                <span className="w-4 text-slate-400 text-xs">{d.rank}</span>
                <LottoBall number={d.number} size="sm" />
                <div className="flex-1">
                  <div className="h-1.5 rounded-full bg-gold/70" style={{ width: `${d.percent}%` }} />
                </div>
                <span className="font-mono text-xs text-slate-400">{d.count}회 ({d.percent.toFixed(1)}%)</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}

function ballBg(n: number): { bg: string; text: string } {
  if (n <= 10) return { bg: '#F9A825', text: '#1A1A2E' }
  if (n <= 20) return { bg: '#1565C0', text: '#FFFFFF' }
  if (n <= 30) return { bg: '#B71C1C', text: '#FFFFFF' }
  if (n <= 40) return { bg: '#424242', text: '#FFFFFF' }
  return { bg: '#1B5E20', text: '#FFFFFF' }
}
