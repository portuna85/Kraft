'use client'

import { useEffect, useState } from 'react'

type Mode = 'auto' | 'desktop' | 'mobile'
const MODES: Mode[] = ['auto', 'desktop', 'mobile']
const LABELS: Record<Mode, string> = { auto: '자동', desktop: '데스크탑', mobile: '모바일' }
const STORAGE_KEY = 'kraft.viewMode'

export default function ViewModeToggle() {
  const [mode, setMode] = useState<Mode>('auto')

  useEffect(() => {
    const saved = (localStorage.getItem(STORAGE_KEY) as Mode) ?? 'auto'
    setMode(saved)
    applyMode(saved)
  }, [])

  function applyMode(m: Mode) {
    const html = document.documentElement
    if (m === 'auto') html.removeAttribute('data-view-mode')
    else html.setAttribute('data-view-mode', m)
  }

  function cycle() {
    const next = MODES[(MODES.indexOf(mode) + 1) % MODES.length]
    setMode(next)
    localStorage.setItem(STORAGE_KEY, next)
    applyMode(next)
  }

  return (
    <button
      onClick={cycle}
      title="화면 모드 전환"
      className="text-xs px-2 py-1 rounded border border-slate-600 text-slate-400 hover:text-gold hover:border-gold transition-colors"
    >
      {LABELS[mode]}
    </button>
  )
}
