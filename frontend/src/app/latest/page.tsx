import type { Metadata } from 'next'
import LatestPage from './LatestPage'

export const metadata: Metadata = {
  title: '최신 당첨 결과 | KRAFT Lotto',
  description: '최신 로또 당첨 번호와 세후 수령액 계산기',
}

export default function Page() {
  return <LatestPage />
}
