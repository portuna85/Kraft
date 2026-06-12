'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { WinningNumberDto, CombinationDto, RuleDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'
import { getDeviceToken } from '@/lib/device'

export default function HomePage() {
  const [latest, setLatest] = useState<WinningNumberDto | null>(null)
  const [count, setCount] = useState(5)
  const [showFilter, setShowFilter] = useState(false)
  const [oddCount, setOddCount] = useState<number | null>(null)
  const [sumMin, setSumMin] = useState<string>('')
  const [sumMax, setSumMax] = useState<string>('')
  const [rules, setRules] = useState<RuleDto[]>([])
  const [result, setResult] = useState<CombinationDto[] | null>(null)
  const [recommending, setRecommending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [savedIdx, setSavedIdx] = useState<Set<number>>(new Set())

  useEffect(() => {
    api.rounds.latest().then(setLatest).catch(() => setLatest(null))
    api.numbers.rules().then(setRules).catch(() => setRules([]))
  }, [])

  async function recommend() {
    setRecommending(true)
    setError(null)
    setSavedIdx(new Set())
    try {
      const res = await api.numbers.recommend({
        count,
        oddCount: oddCount ?? undefined,
        sumMin: sumMin ? Number(sumMin) : undefined,
        sumMax: sumMax ? Number(sumMax) : undefined,
      })
      setResult(res.combinations)
    } catch (e) {
      setError(e instanceof Error ? e.message : '오류가 발생했습니다')
    } finally {
      setRecommending(false)
    }
  }

  async function saveCombo(idx: number, numbers: number[]) {
    const token = getDeviceToken()
    if (!token) return
    try {
      await api.saved.save(token, numbers)
      setSavedIdx((prev) => new Set(prev).add(idx))
    } catch {
      // 저장 실패 무시
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
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">번호 추천</h2>
          <button
            className="text-xs text-slate-400 hover:text-white"
            onClick={() => setShowFilter((v) => !v)}
          >
            {showFilter ? '필터 접기 ▲' : '필터 설정 ▼'}
          </button>
        </div>

        {/* 필터 */}
        {showFilter && (
          <div className="bg-navy rounded p-3 space-y-3 text-sm">
            <div className="flex flex-wrap gap-4">
              <div className="space-y-1">
                <label className="text-slate-400 text-xs">홀수 개수</label>
                <div className="flex gap-1 flex-wrap">
                  {[null, 1, 2, 3, 4, 5, 6].map((v) => (
                    <button
                      key={String(v)}
                      onClick={() => setOddCount(v)}
                      className={`px-2 py-0.5 rounded text-xs ${
                        oddCount === v ? 'bg-gold text-navy font-bold' : 'bg-[#16213E] text-slate-400 hover:text-white'
                      }`}
                    >
                      {v === null ? '전체' : `홀 ${v}`}
                    </button>
                  ))}
                </div>
              </div>
              <div className="space-y-1">
                <label className="text-slate-400 text-xs">합산 범위</label>
                <div className="flex items-center gap-2">
                  <input
                    type="number" placeholder="최소" value={sumMin}
                    onChange={(e) => setSumMin(e.target.value)}
                    className="w-16 bg-[#16213E] border border-[#0F3460] rounded px-2 py-0.5 text-xs text-white"
                  />
                  <span className="text-slate-400">~</span>
                  <input
                    type="number" placeholder="최대" value={sumMax}
                    onChange={(e) => setSumMax(e.target.value)}
                    className="w-16 bg-[#16213E] border border-[#0F3460] rounded px-2 py-0.5 text-xs text-white"
                  />
                </div>
              </div>
            </div>
            {rules.length > 0 && (
              <div className="text-xs text-slate-500">
                적용 규칙: {rules.map((r) => r.name).join(', ')}
              </div>
            )}
          </div>
        )}

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
                <button
                  onClick={() => saveCombo(i, c.numbers)}
                  disabled={savedIdx.has(i)}
                  className={`ml-auto text-xs px-2 py-0.5 rounded transition-colors ${
                    savedIdx.has(i)
                      ? 'text-slate-500 cursor-default'
                      : 'text-slate-400 hover:text-gold hover:bg-gold/10'
                  }`}
                >
                  {savedIdx.has(i) ? '저장됨' : '저장'}
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
