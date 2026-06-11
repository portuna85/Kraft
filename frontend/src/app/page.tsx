'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { WinningNumberDto, CombinationDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'

export default function HomePage() {
  const [latest, setLatest] = useState<WinningNumberDto | null>(null)
  const [count, setCount] = useState(5)
  const [result, setResult] = useState<CombinationDto[] | null>(null)
  const [recommending, setRecommending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.rounds.latest().then(setLatest).catch(() => setLatest(null))
  }, [])

  async function recommend() {
    setRecommending(true)
    setError(null)
    try {
      const res = await api.numbers.recommend({ count })
      setResult(res.combinations)
    } catch (e) {
      setError(e instanceof Error ? e.message : '오류가 발생했습니다')
    } finally {
      setRecommending(false)
    }
  }

  return (
    <div className="space-y-6">
      {/* 히어로 */}
      <header className="text-center space-y-2 py-4">
        <p className="eyebrow">KRAFT Lotto</p>
        <h1 className="text-2xl font-bold">로또 번호 분석 도구</h1>
        <p className="text-slate-400 text-sm">
          과거 당첨 데이터를 기반으로 번호 조합을 분석합니다.
        </p>
      </header>

      {/* 최신 회차 */}
      {latest && (
        <section data-testid="latest-draw" className="card space-y-3" aria-label={`제 ${latest.round}회 당첨번호`}>
          <div className="flex items-center justify-between">
            <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">
              제 {latest.round}회 당첨번호
            </h2>
            <span className="text-xs text-slate-400">{latest.drawDate}</span>
          </div>
          <div className="flex items-center gap-2 flex-wrap">
            {latest.numbers.map((n) => <LottoBall key={n} number={n} />)}
            <span className="text-slate-400 text-sm">+</span>
            <LottoBall number={latest.bonusNumber} bonus />
          </div>
          <p className="text-xs text-slate-400">
            1등 {latest.firstWinners}명 · {latest.firstPrize.toLocaleString()}원
          </p>
        </section>
      )}

      <AdSlot slotId="home-mid" />

      {/* 번호 추천 */}
      <section data-testid="recommend-section" className="card space-y-4" aria-label="번호 추천">
        <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">번호 추천</h2>
        <div className="space-y-2">
          <label htmlFor="count-range" className="text-sm text-slate-300">조합 수: {count}줄</label>
          <input
            id="count-range"
            type="range" min={1} max={10} value={count}
            onChange={(e) => setCount(Number(e.target.value))}
            className="w-full accent-gold"
          />
        </div>
        <button className="btn-primary w-full" onClick={recommend} disabled={recommending}>
          {recommending ? '생성 중…' : '번호 생성'}
        </button>
        {error && <p className="text-red-400 text-sm">{error}</p>}
        {result && (
          <ul className="space-y-2 mt-2">
            {result.map((c, i) => (
              <li key={i} className="flex items-center gap-2 flex-wrap">
                <span className="text-slate-400 text-sm w-5">{i + 1}</span>
                {c.numbers.map((n) => <LottoBall key={n} number={n} />)}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
