'use client'

import { useEffect, useState } from 'react'
import { api } from '@/lib/api'
import type { SavedNumbersDto } from '@/lib/types'
import LottoBall from '@/components/LottoBall'
import { getDeviceToken } from '@/lib/device'

export default function SavedPage() {
  const [list, setList] = useState<SavedNumbersDto[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState<Set<number>>(new Set())
  const [copied, setCopied] = useState<number | null>(null)

  useEffect(() => {
    const token = getDeviceToken()
    api.saved.list(token)
      .then(setList)
      .catch(() => setList([]))
      .finally(() => setLoading(false))
  }, [])

  async function remove(id: number) {
    const token = getDeviceToken()
    setDeleting((prev) => new Set(prev).add(id))
    try {
      await api.saved.delete(id, token)
      setList((prev) => prev?.filter((s) => s.id !== id) ?? prev)
    } finally {
      setDeleting((prev) => { const next = new Set(prev); next.delete(id); return next })
    }
  }

  async function copyNumbers(id: number, numbers: number[]) {
    try {
      await navigator.clipboard.writeText(numbers.join(', '))
      setCopied(id)
      setTimeout(() => setCopied((prev) => prev === id ? null : prev), 1500)
    } catch {
      // clipboard 권한 없음 — 무시
    }
  }

  function formatDate(iso: string) {
    return new Date(iso).toLocaleString('ko-KR', {
      month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
    })
  }

  return (
    <div data-testid="saved-page" className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">저장함</p>
        <h1 className="text-2xl font-bold">저장된 번호</h1>
        <p className="text-slate-400 text-sm">이 기기에서 저장한 번호 조합입니다.</p>
      </header>

      {loading && <p className="text-slate-400 text-center py-12">불러오는 중…</p>}

      {!loading && list !== null && list.length === 0 && (
        <p className="text-slate-400 text-center py-12">저장된 번호가 없습니다.</p>
      )}

      {list && list.length > 0 && (
        <ul className="divide-y divide-[#0F3460]">
          {list.map((s) => (
            <li key={s.id} className="py-4 flex items-center gap-3 flex-wrap">
              <div className="flex-1 space-y-2">
                <div className="flex items-center gap-1.5 flex-wrap">
                  {s.numbers.map((n) => <LottoBall key={n} number={n} size="sm" />)}
                </div>
                <div className="flex items-center gap-3 text-xs text-slate-500">
                  {s.label && <span className="text-slate-400">{s.label}</span>}
                  <span>{formatDate(s.savedAt)}</span>
                </div>
              </div>
              <div className="flex gap-2 shrink-0">
                <button
                  onClick={() => copyNumbers(s.id, s.numbers)}
                  className="text-xs text-slate-500 hover:text-gold transition-colors"
                >
                  {copied === s.id ? '복사됨 ✓' : '복사'}
                </button>
                <button
                  onClick={() => remove(s.id)}
                  disabled={deleting.has(s.id)}
                  className="text-xs text-slate-500 hover:text-red-400 transition-colors disabled:opacity-30"
                >
                  {deleting.has(s.id) ? '삭제 중…' : '삭제'}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
