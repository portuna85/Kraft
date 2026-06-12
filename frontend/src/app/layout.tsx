import type { Metadata } from 'next'
import Script from 'next/script'
import Link from 'next/link'
import Header from '@/components/Header'
import './globals.css'

export const metadata: Metadata = {
  title: 'KRAFT Lotto — 번호 조합 분석 도구',
  description: '로또 번호 추천, 출현 빈도, 조합 분석 도구',
}

const adsenseId = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        {adsenseId && (
          <Script
            async
            src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${adsenseId}`}
            crossOrigin="anonymous"
            strategy="lazyOnload"
          />
        )}
      </head>
      <body className="min-h-screen flex flex-col">
        <Header />
        <main className="flex-1 py-6">
          <div className="page-container">{children}</div>
        </main>
        <footer className="border-t border-[#0F3460] py-6 text-center text-xs text-slate-400">
          <div className="page-container space-y-1">
            <p>본 서비스는 당첨 번호를 예측하지 않습니다. 과거 통계 데이터 기반 참고 도구입니다.</p>
            <p>
              <Link href="/info/privacy" className="underline hover:text-slate-200">개인정보처리방침</Link>
              {' · '}
              <Link href="/info/terms" className="underline hover:text-slate-200">이용약관</Link>
              {' · '}
              <Link href="/info/faq" className="underline hover:text-slate-200">FAQ</Link>
            </p>
          </div>
        </footer>
      </body>
    </html>
  )
}
