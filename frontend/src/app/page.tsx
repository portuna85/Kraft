import type { Metadata } from 'next'
import HomePage from './HomePage'

export const metadata: Metadata = {
  title: 'KRAFT Lotto — 로또 번호 분석 도구',
  description: '과거 당첨 데이터를 기반으로 번호 조합을 분석하는 로또 통계 도구',
}

export default function Page() {
  return <HomePage />
}
