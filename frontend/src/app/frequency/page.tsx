import type { Metadata } from 'next'
import FrequencyPage from './FrequencyPage'

export const metadata: Metadata = {
  title: '번호별 출현 빈도 | KRAFT Lotto',
  description: '로또 번호별 역대 출현 빈도 통계',
}

export default function Page() {
  return <FrequencyPage />
}
