'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { api } from '@/lib/api'
import type { WinningNumberDto, WinningNumberPageDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import AdSlot from '@/components/AdSlot'

export default function RoundsPage() {
  const router = useRouter()
  const [data, setData] = useState<WinningNumberPageDto | null>(null)
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    api.rounds.list(page)
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false))
  }, [page])

  function onSearch() {
    const n = parseInt(search, 10)
    if (n > 0) router.push(`/rounds/${n}`)
  }

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
      onClick={() => router.push(`/rounds/${r.round}`)}
    >
      <span className="text-sm font-semibold w-12 shrink-0">제 {r.round}회</span>
      <div className="flex gap-1 flex-wrap flex-1">
        {r.numbers.map((n) => <LottoBall key={n} number={n} size="sm" />)}
        <span className="text-slate-500 text-xs self-center">+</span>
        <LottoBall number={r.bonusNumber} size="sm" bonus />
      </div>
      <span className="text-xs text-slate-500 shrink-0">{r.drawDate}</span>
      <span className="text-slate-500">›</span>
    </li>
  )
}
