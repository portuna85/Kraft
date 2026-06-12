import type { Metadata } from 'next'
import SavedPage from './SavedPage'

export const metadata: Metadata = {
  title: '저장된 번호 | KRAFT Lotto',
  description: '이 기기에서 저장한 로또 번호 조합',
}

export default function Page() {
  return <SavedPage />
}
