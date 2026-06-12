'use client'

import { useState } from 'react'
import { api } from '@/lib/api'
import type { CombinationPrizeHistoryDto } from '@/lib/types'
import { getDeviceToken } from '@/lib/device'
import LottoBall from '@/components/LottoBall'
import BallGrid from '@/components/BallGrid'
import AdSlot from '@/components/AdSlot'

export default function AnalysisPage() {
  const [selected, setSelected] = useState<number[]>([])
  const [result, setResult] = useState<CombinationPrizeHistoryDto | null>(null)
  const [loading, setLoading] = useState(false)
  const [saved, setSaved] = useState(false)
  const [analyzeError, setAnalyzeError] = useState<string | null>(null)

  function toggle(n: number) {
    setSelected((prev) =>
      prev.includes(n) ? prev.filter((x) => x !== n) : prev.length < 6 ? [...prev, n] : prev
    )
    setResult(null)
    setSaved(false)
  }

  async function saveNumbers() {
    const token = getDeviceToken()
    try {
      await api.saved.save(token, selected)
      setSaved(true)
    } catch {
      // 저장 실패 무시
    }
  }

  async function analyze() {
    if (selected.length !== 6) return
    setLoading(true)
    setAnalyzeError(null)
    try {
      setResult(await api.stats.analysis(selected))
    } catch (e) {
      setResult(null)
      setAnalyzeError(e instanceof Error ? e.message : '분석 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div data-testid="analysis-page" className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">번호 조합 분석</p>
        <h1 className="text-2xl font-bold">번호 조합 분석</h1>
        <p className="text-slate-400 text-sm">6개 선택 시 당첨 이력을 분석합니다.</p>
      </header>

      <AdSlot slotId="analysis-top" />

      <div className="card space-y-4">
        <div className="flex gap-2 flex-wrap min-h-10">
          {selected.length === 0
            ? <span className="text-slate-400 text-sm">번호를 6개 선택하세요</span>
            : selected.map((n) => <LottoBall key={n} number={n} size="sm" />)}
        </div>

        <BallGrid selected={selected} onToggle={toggle} max={6} />

        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">{selected.length}/6 선택</span>
          <div className="flex gap-2">
            <button
              className="px-3 py-1 rounded text-sm bg-[#16213E] text-slate-400 hover:text-white"
              onClick={() => { setSelected([]); setResult(null); setSaved(false) }}
            >초기화</button>
            {selected.length === 6 && (
              <button
                onClick={saveNumbers}
                disabled={saved}
                className={`px-3 py-1 rounded text-sm transition-colors ${
                  saved ? 'text-slate-500 cursor-default' : 'text-slate-400 hover:text-gold hover:bg-gold/10'
                }`}
              >{saved ? '저장됨' : '저장'}</button>
            )}
            <button
              className="btn-primary px-4 py-1.5 text-sm"
              onClick={analyze}
              disabled={selected.length !== 6 || loading}
            >{loading ? '분석 중…' : '분석'}</button>
          </div>
        </div>
      </div>

      {analyzeError && <p className="text-red-400 text-sm">{analyzeError}</p>}

      {result && (
        <section className="card space-y-3">
          <h2 className="font-semibold">분석 결과</h2>
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div className="bg-navy rounded p-3">
              <dt className="text-xs text-slate-400">1등 당첨 이력</dt>
              <dd className="text-2xl font-bold text-gold mt-1">{result.firstPrizeCount}회</dd>
            </div>
            <div className="bg-navy rounded p-3">
              <dt className="text-xs text-slate-400">2등 당첨 이력</dt>
              <dd className="text-2xl font-bold text-gold mt-1">{result.secondPrizeCount}회</dd>
            </div>
          </dl>
          {result.firstPrizeHits.length > 0 && (
            <div>
              <h3 className="text-xs text-slate-400 mb-1">1등 회차</h3>
              <ul className="text-sm space-y-0.5">
                {result.firstPrizeHits.map((h) => (
                  <li key={h.round} className="text-slate-300">
                    {h.round}회 ({h.drawDate})
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
