'use client'

import { useState } from 'react'
import { api } from '@/lib/api'
import type { CombinationPrizeHistoryDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'

export default function AnalysisPage() {
  const [selected, setSelected] = useState<number[]>([])
  const [result, setResult] = useState<CombinationPrizeHistoryDto | null>(null)
  const [loading, setLoading] = useState(false)

  function toggle(n: number) {
    setSelected((prev) =>
      prev.includes(n) ? prev.filter((x) => x !== n) : prev.length < 6 ? [...prev, n] : prev
    )
    setResult(null)
  }

  async function analyze() {
    if (selected.length !== 6) return
    setLoading(true)
    try {
      setResult(await api.stats.analysis(selected))
    } catch {
      setResult(null)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">번호 조합 분석</p>
        <h1 className="text-2xl font-bold">번호 조합 분석</h1>
        <p className="text-slate-400 text-sm">6개 선택 시 당첨 이력을 분석합니다.</p>
      </header>

      <AdSlot slotId="analysis-top" />

      <div className="card space-y-4">
        <div className="flex gap-2 flex-wrap min-h-10">
          {selected.length === 0
            ? <span className="text-slate-500 text-sm">번호를 6개 선택하세요</span>
            : selected.map((n) => <LottoBall key={n} number={n} />)}
        </div>

        <div className="grid grid-cols-9 gap-1">
          {Array.from({ length: 45 }, (_, i) => i + 1).map((n) => (
            <button
              key={n}
              onClick={() => toggle(n)}
              className={`aspect-square rounded-full text-xs font-bold transition-all ${
                selected.includes(n)
                  ? 'ring-2 ring-gold scale-110'
                  : selected.length >= 6
                  ? 'opacity-30 cursor-not-allowed'
                  : 'hover:scale-105'
              }`}
              style={{
                backgroundColor: ballBg(n),
                color: '#fff',
              }}
            >
              {n}
            </button>
          ))}
        </div>

        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">{selected.length}/6 선택</span>
          <div className="flex gap-2">
            <button
              className="px-3 py-1 rounded text-sm bg-[#16213E] text-slate-400 hover:text-white"
              onClick={() => { setSelected([]); setResult(null) }}
            >초기화</button>
            <button
              className="btn-primary px-4 py-1.5 text-sm"
              onClick={analyze}
              disabled={selected.length !== 6 || loading}
            >{loading ? '분석 중…' : '분석'}</button>
          </div>
        </div>
      </div>

      {result && (
        <section className="card space-y-3">
          <h2 className="font-semibold">분석 결과</h2>
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-navy rounded p-3">
              <dt className="text-xs text-slate-500">1등 당첨 이력</dt>
              <dd className="text-2xl font-bold text-gold mt-1">{result.firstPrizeCount}회</dd>
            </div>
            <div className="bg-navy rounded p-3">
              <dt className="text-xs text-slate-500">2등 당첨 이력</dt>
              <dd className="text-2xl font-bold text-gold mt-1">{result.secondPrizeCount}회</dd>
            </div>
          </dl>
          {result.firstPrizeHits.length > 0 && (
            <div>
              <h3 className="text-xs text-slate-500 mb-1">1등 회차</h3>
              <ul className="text-sm space-y-0.5">
                {result.firstPrizeHits.map((h) => (
                  <li key={h.round} className="text-slate-300">
                    제 {h.round}회 ({h.drawDate})
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      )}
    </div>
  )
}

function ballBg(n: number) {
  if (n <= 10) return '#F9A825'
  if (n <= 20) return '#42A5F5'
  if (n <= 30) return '#EF5350'
  if (n <= 40) return '#757575'
  return '#66BB6A'
}
