'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import type { WinningNumberDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'

export default function RoundDetailPage() {
  const { round } = useParams<{ round: string }>()
  const router = useRouter()
  const [data, setData] = useState<WinningNumberDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)

  useEffect(() => {
    const n = parseInt(round, 10)
    if (!n) { setNotFound(true); setLoading(false); return }
    api.rounds.get(n)
      .then(setData)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false))
  }, [round])

  if (loading) return <p className="text-slate-400 text-center py-12">불러오는 중…</p>
  if (notFound || !data) return (
    <div className="text-center py-12 space-y-4">
      <p className="text-slate-400">회차를 찾을 수 없습니다.</p>
      <button className="btn-primary" onClick={() => router.push('/rounds')}>목록으로</button>
    </div>
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <button onClick={() => router.push('/rounds')} className="text-slate-400 hover:text-white">‹</button>
        <header>
          <p className="eyebrow">회차 상세</p>
          <h1 className="text-2xl font-bold">제 {data.round}회</h1>
        </header>
      </div>

      <section className="card space-y-4">
        <span className="text-sm text-slate-500">{data.drawDate} 추첨</span>
        <div className="flex items-center gap-2 flex-wrap">
          {data.numbers.map((n) => <LottoBall key={n} number={n} size="lg" />)}
          <span className="text-slate-400">+</span>
          <LottoBall number={data.bonusNumber} size="lg" bonus />
        </div>
        <dl className="grid grid-cols-2 gap-3 text-sm">
          {[
            ['1등 당첨금', `${data.firstPrize.toLocaleString()}원`],
            ['당첨자 수', `${data.firstWinners}명`],
            ['총 판매액', `${data.totalSales.toLocaleString()}원`],
          ].map(([k, v]) => (
            <div key={k} className="bg-navy rounded p-2">
              <dt className="text-xs text-slate-500">{k}</dt>
              <dd className="font-mono text-xs mt-0.5">{v}</dd>
            </div>
          ))}
        </dl>
      </section>
    </div>
  )
}
