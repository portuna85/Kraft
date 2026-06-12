'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { WinningNumberDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'

function taxInfo(prize: number): { rate: string; tax: number; after: number } {
  if (prize <= 0) return { rate: '-', tax: 0, after: 0 }
  const rate = prize > 300_000_000 ? 0.33 : 0.22
  const tax = Math.floor(prize * rate)
  return { rate: `${rate * 100}%`, tax, after: prize - tax }
}

export default function LatestPage() {
  const [data, setData] = useState<WinningNumberDto | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.rounds.latest()
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [])

  const first = data ? taxInfo(data.firstPrize) : null
  const second = data ? taxInfo(data.secondPrize) : null

  return (
    <div data-testid="latest-page" className="space-y-6">
      {loading ? (
        <p className="text-slate-400 text-center py-12">불러오는 중…</p>
      ) : !data ? (
        <p className="text-slate-400 text-center py-12">데이터가 없습니다.</p>
      ) : (
        <>
          <header className="space-y-1">
            <p className="eyebrow">최신 회차</p>
            <h1 className="text-2xl font-bold">최신 당첨 결과</h1>
          </header>

          <AdSlot slotId="latest-top" />

          <section className="card space-y-4">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <h2 className="font-semibold">{data.round}회 당첨번호</h2>
              <span className="text-sm text-slate-400">{data.drawDate} 추첨</span>
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              {data.numbers.map((n) => <LottoBall key={n} number={n} size="lg" />)}
              <span className="text-slate-400">+</span>
              <LottoBall number={data.bonusNumber} size="lg" bonus />
            </div>
            <dl className="grid grid-cols-3 gap-3 text-sm">
              {[
                ['1등 당첨금', `${data.firstPrize.toLocaleString()}원`],
                ['당첨자 수', `${data.firstWinners}명`],
                ['총 판매액', `${data.totalSales.toLocaleString()}원`],
              ].map(([k, v]) => (
                <div key={k} className="bg-navy rounded p-2">
                  <dt className="text-xs text-slate-400">{k}</dt>
                  <dd className="font-mono text-xs mt-0.5">{v}</dd>
                </div>
              ))}
            </dl>
          </section>

          <section className="card space-y-4">
            <h2 className="font-semibold text-sm text-slate-400 uppercase tracking-wide">
              세후 예상 수령액
            </h2>
            <p className="text-xs text-slate-400">3억 초과 33%, 200만 초과 22% 원천징수 적용</p>
            <div className="grid md:grid-cols-2 gap-4">
              {[
                { rank: '1등', prize: data.firstPrize, winners: data.firstWinners, info: first! },
                { rank: '2등', prize: data.secondPrize, winners: data.secondWinners, info: second! },
              ].map(({ rank, prize, winners, info }) => (
                <dl key={rank} className="bg-navy rounded p-3 space-y-1 text-sm">
                  <dt className="font-bold text-gold">{rank}</dt>
                  {[
                    ['세전 1인당', `${prize.toLocaleString()}원`],
                    ['세율', info.rate],
                    ['세금 공제', `${info.tax.toLocaleString()}원`],
                    ['세후 수령액', `${info.after.toLocaleString()}원`],
                    ['당첨자 수', `${winners}명`],
                  ].map(([k, v]) => (
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
    </div>
  )
}
