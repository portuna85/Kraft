'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import ViewModeToggle from './ViewModeToggle'

const NAV = [
  { href: '/', label: '홈' },
  { href: '/latest', label: '최신 결과' },
  { href: '/rounds', label: '회차 검색' },
  { href: '/frequency', label: '출현 빈도' },
  { href: '/analysis', label: '번호 분석' },
  { href: '/companion', label: '동반 번호' },
  { href: '/stats', label: '패턴 통계' },
  { href: '/saved', label: '저장함' },
]

export default function Header() {
  const path = usePathname()

  return (
    <header className="sticky top-0 z-50 border-b border-[#0F3460] bg-[#1A1A2E]/90 backdrop-blur">
      <div className="page-container flex items-center justify-between h-14 gap-4">
        <Link href="/" className="font-bold text-lg text-gold shrink-0">
          KRAFT Lotto
        </Link>

        <nav className="hidden md:flex items-center gap-1 overflow-x-auto">
          {NAV.map(({ href, label }) => (
            <Link
              key={href}
              href={href}
              className={`px-3 py-1.5 rounded text-sm transition-colors whitespace-nowrap ${
                path === href
                  ? 'bg-gold/10 text-gold font-semibold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              {label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2 shrink-0">
          <ViewModeToggle />
        </div>
      </div>

      {/* 모바일 하단 탭 */}
      <nav data-testid="bottom-nav" className="md:hidden flex border-t border-[#0F3460] overflow-x-auto">
        {NAV.map(({ href, label }) => (
          <Link
            key={href}
            href={href}
            className={`flex-1 min-w-fit px-3 py-2 text-center text-xs whitespace-nowrap transition-colors ${
              path === href ? 'text-gold border-b-2 border-gold' : 'text-slate-400'
            }`}
          >
            {label}
          </Link>
        ))}
      </nav>
    </header>
  )
}
