import type { Metadata } from 'next'
import StatsPage from './StatsPage'

export const metadata: Metadata = {
  title: '패턴 통계 | KRAFT Lotto',
  description: '로또 당첨 번호의 홀짝·합산 범위 패턴 통계',
}

export default function Page() {
  return <StatsPage />
}
