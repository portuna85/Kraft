'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { WinningNumberDto, CombinationDto, RuleDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'
import { getDeviceToken } from '@/lib/device'

function taxInfo(prize: number) {
  if (prize <= 0) return { rate: '-', tax: 0, after: 0 }
  const rate = prize > 300_000_000 ? 0.33 : 0.22
  const tax = Math.floor(prize * rate)
  return { rate: `${rate * 100}%`, tax, after: prize - tax }
}

const RULE_LABELS: Record<string, string> = {
  ArithmeticSequenceRule: '등차수열 제외',
  BirthdayBiasRule: '생일 편향 제외',
  LongRunRule: '연속 번호 제외',
  PastWinningRule: '과거 당첨 제외',
  SingleDecadeRule: '십의 자리 편중 제외',
}

export default function HomePage() {
  const [latest, setLatest] = useState<WinningNumberDto | null>(null)
  const [latestLoading, setLatestLoading] = useState(true)
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
    api.rounds.latest()
      .then(setLatest)
      .catch(() => setLatest(null))
      .finally(() => setLatestLoading(false))
    api.numbers.rules().then(setRules).catch(() => setRules([]))
    setRecommending(true)
    api.numbers.recommend({ count: 5 })
      .then(res => setResult(res.combinations))
      .catch(e => setError(e instanceof Error ? e.message : '오류가 발생했습니다'))
      .finally(() => setRecommending(false))
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

  const first = latest ? taxInfo(latest.firstPrize) : null
  const second = latest ? taxInfo(latest.secondPrize) : null

  return (
    <div className="space-y-6">
      {/* 최신 당첨 결과 */}
      <header className="space-y-1">
        <p className="eyebrow">최신 당첨 결과</p>
        <h1 className="text-2xl font-bold">
          {latestLoading ? '불러오는 중…' : latest ? `${latest.round}회 당첨번호` : '최신 결과 없음'}
        </h1>
      </header>

      {!latestLoading && !latest && (
        <p className="text-slate-400 text-center py-6">데이터가 없습니다.</p>
      )}

      {latest && (
        <>
          <section data-testid="latest-draw" className="card space-y-4" aria-label={`${latest.round}회 당첨번호`}>
            <div className="flex items-center justify-between flex-wrap gap-2">
              <h2 className="font-semibold">{latest.round}회 당첨번호</h2>
              <span className="text-sm text-slate-400">{latest.drawDate} 추첨</span>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              {latest.numbers.map((n) => <LottoBall key={n} number={n} />)}
              <span className="text-slate-400">+</span>
              <LottoBall number={latest.bonusNumber} bonus />
            </div>
            <dl className="grid grid-cols-3 gap-3 text-sm">
              {([
                ['1등 당첨금', `${latest.firstPrize.toLocaleString()}원`],
                ['당첨자 수', `${latest.firstWinners}명`],
                ['총 판매액', `${latest.totalSales.toLocaleString()}원`],
              ] as [string, string][]).map(([k, v]) => (
                <div key={k} className="bg-navy rounded p-2">
                  <dt className="text-xs text-slate-400">{k}</dt>
                  <dd className="font-mono text-xs mt-0.5">{v}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="card space-y-3">
            <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">세후 예상 수령액</h2>
            <p className="text-xs text-slate-400">3억 초과 33%, 200만 초과 22% 원천징수 적용</p>
            <div className="grid md:grid-cols-2 gap-4">
              {([
                { rank: '1등', prize: latest.firstPrize, winners: latest.firstWinners, info: first! },
                { rank: '2등', prize: latest.secondPrize, winners: latest.secondWinners, info: second! },
              ]).map(({ rank, prize, winners, info }) => (
                <dl key={rank} className="bg-navy rounded p-3 space-y-1 text-sm">
                  <dt className="font-bold text-gold">{rank}</dt>
                  {([
                    ['세전 1인당', `${prize.toLocaleString()}원`],
                    ['세율', info.rate],
                    ['세금 공제', `${info.tax.toLocaleString()}원`],
                    ['세후 수령액', `${info.after.toLocaleString()}원`],
                    ['당첨자 수', `${winners}명`],
                  ] as [string, string][]).map(([k, v]) => (
                    <div key={k} className="flex justify-between">
                      <span className="text-slate-400">{k}</span>
                      <span className="font-mono">{v}</span>
                    </div>
                  ))}
                </dl>
              ))}
            </div>
          </section>
        </>
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
                적용 규칙: {rules.map((r) => RULE_LABELS[r.name] ?? r.name).join(' · ')}
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
