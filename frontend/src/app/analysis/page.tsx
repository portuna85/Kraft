import type { Metadata } from 'next'
import AnalysisPage from './AnalysisPage'

export const metadata: Metadata = {
  title: '번호 조합 분석 | KRAFT Lotto',
  description: '6개 번호 조합의 역대 당첨 이력 분석',
}

export default function Page() {
  return <AnalysisPage />
}
