import type { Metadata } from 'next'
import CompanionPage from './CompanionPage'

export const metadata: Metadata = {
  title: '동반 출현 기록 | KRAFT Lotto',
  description: '특정 번호와 함께 자주 출현한 번호 조회',
}

export default function Page() {
  return <CompanionPage />
}
