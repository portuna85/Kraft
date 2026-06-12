'use client'

import { useEffect, useState, Suspense } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { api } from '@/lib/api'
import type { WinningNumberDto, WinningNumberPageDto } from '@/lib/types'
import Link from 'next/link'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'

function RoundsContent() {
  const router = useRouter()
  const params = useSearchParams()
  const idParam = params.get('id')

  const [data, setData] = useState<WinningNumberPageDto | null>(null)
  const [detail, setDetail] = useState<WinningNumberDto | null>(null)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)

  // 상세 모드
  useEffect(() => {
    if (!idParam) { setDetail(null); setNotFound(false); return }
    const n = parseInt(idParam, 10)
    if (!n) { setNotFound(true); return }
    setLoading(true)
    api.rounds.get(n)
      .then(setDetail)
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false))
  }, [idParam])

  // 목록 모드
  useEffect(() => {
    if (idParam) return
    setLoading(true)
    api.rounds.list(page)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [page, idParam])

  function onSearch() {
    const n = parseInt(search, 10)
    if (n > 0) router.push(`/rounds?id=${n}`)
  }

  // 상세 뷰
  if (idParam) {
    if (loading) return <p className="text-slate-400 text-center py-12">불러오는 중…</p>
    if (notFound || !detail) return (
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
            <h1 className="text-2xl font-bold">{detail.round}회</h1>
          </header>
        </div>
        <section className="card space-y-4">
          <span className="text-sm text-slate-400">{detail.drawDate} 추첨</span>
          <div className="flex items-center gap-2 flex-wrap">
            {detail.numbers.map((n) => <LottoBall key={n} number={n} size="lg" />)}
            <span className="text-slate-400">+</span>
            <LottoBall number={detail.bonusNumber} size="lg" bonus />
          </div>
          <dl className="grid grid-cols-2 gap-3 text-sm">
            {[
              ['1등 당첨금', `${detail.firstPrize.toLocaleString()}원`],
              ['1등 당첨자', `${detail.firstWinners}명`],
              ['2등 당첨금', `${detail.secondPrize.toLocaleString()}원`],
              ['2등 당첨자', `${detail.secondWinners}명`],
              ['총 판매액', `${detail.totalSales.toLocaleString()}원`],
            ].map(([k, v]) => (
              <div key={k} className="bg-navy rounded p-2">
                <dt className="text-xs text-slate-400">{k}</dt>
                <dd className="font-mono text-xs mt-0.5">{v}</dd>
              </div>
            ))}
          </dl>
          <Link
            href="/analysis"
            className="inline-block text-xs text-slate-400 hover:text-gold transition-colors"
          >
            이 번호 조합 분석하기 →
          </Link>
        </section>
      </div>
    )
  }

  // 목록 뷰
  return (
    <div data-testid="rounds-page" className="space-y-4">
      <header className="space-y-1">
        <p className="eyebrow">회차 검색</p>
        <h1 className="text-2xl font-bold">회차 검색</h1>
      </header>

      <div className="flex gap-2">
        <input
          type="number" value={search} onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && onSearch()}
          placeholder="회차 번호 입력 (예: 1200)"
          className="flex-1 bg-[#16213E] border border-[#0F3460] rounded px-3 py-2 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-gold"
        />
        <button className="btn-primary px-4" onClick={onSearch}>이동</button>
      </div>

      <AdSlot slotId="rounds-top" />

      {loading && <p className="text-slate-400 text-center py-8">불러오는 중…</p>}
      {data && (
        <>
          <ul className="divide-y divide-[#0F3460]">
            {data.content.map((r) => (
              <RoundRow key={r.round} round={r} />
            ))}
          </ul>
          <div className="flex justify-center items-center gap-4 py-2">
            <button
              className="text-slate-400 hover:text-white disabled:opacity-30"
              onClick={() => setPage((p) => p - 1)} disabled={page === 0}
            >‹ 이전</button>
            <span className="text-sm text-slate-400">{page + 1} / {data.totalPages}</span>
            <button
              className="text-slate-400 hover:text-white disabled:opacity-30"
              onClick={() => setPage((p) => p + 1)} disabled={page >= data.totalPages - 1}
            >다음 ›</button>
          </div>
        </>
      )}
    </div>
  )
}

function RoundRow({ round: r }: { round: WinningNumberDto }) {
  const router = useRouter()
  return (
    <li
      className="py-3 flex items-center justify-between gap-3 cursor-pointer hover:bg-[#16213E] rounded px-2 transition-colors"
      onClick={() => router.push(`/rounds?id=${r.round}`)}
    >
      <span className="text-sm font-semibold w-12 shrink-0">{r.round}회</span>
      <div className="flex gap-1 flex-wrap flex-1">
        {r.numbers.map((n) => <LottoBall key={n} number={n} size="sm" />)}
        <span className="text-slate-400 text-xs self-center">+</span>
        <LottoBall number={r.bonusNumber} size="sm" bonus />
      </div>
      <span className="text-xs text-slate-400 shrink-0">{r.drawDate}</span>
      <span className="text-slate-400">›</span>
    </li>
  )
}

export default function RoundsPage() {
  return (
    <Suspense fallback={<p className="text-slate-400 text-center py-12">불러오는 중…</p>}>
      <RoundsContent />
    </Suspense>
  )
}
