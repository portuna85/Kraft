import type { Metadata } from 'next'
import RoundsPage from './RoundsPage'

export const metadata: Metadata = {
  title: '회차 검색 | KRAFT Lotto',
  description: '역대 로또 회차별 당첨 번호 검색',
}

export default function Page() {
  return <RoundsPage />
}
