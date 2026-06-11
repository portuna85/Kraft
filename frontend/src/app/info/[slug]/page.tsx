import { notFound } from 'next/navigation'
import AdSlot from '@/components/AdSlot'
import DataSourcePage from '@/components/DataSourcePage'

// --- 정적 페이지 데이터 ---

function QA({ q, children }: { q: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1">
      <p className="font-semibold text-white">Q. {q}</p>
      <p className="text-slate-400">{children}</p>
    </div>
  )
}

const STATIC_PAGES: Record<string, { title: string; content: React.ReactNode }> = {
  faq: {
    title: 'FAQ',
    content: (
      <div className="space-y-4 text-sm text-slate-300">
        <QA q="이 사이트가 당첨번호를 예측하나요?">
          아닙니다. 과거 데이터와 조합 규칙을 참고하는 도구이며 당첨 확률을 높이지 않습니다.
        </QA>
        <QA q="번호 추천이 실제로 유리한가요?">
          통계적으로 모든 조합의 당첨 확률은 동일합니다. 추천은 흔한 선택 패턴을 피하는 참고용입니다.
        </QA>
        <QA q="데이터는 어디서 가져오나요?">
          동행복권 공식 발표 데이터를 기반으로 합니다.
        </QA>
        <QA q="모바일에서도 사용할 수 있나요?">
          네, 반응형으로 설계되어 모바일 브라우저에서도 이용 가능합니다.
        </QA>
      </div>
    ),
  },
  privacy: {
    title: '개인정보처리방침',
    content: (
      <div className="space-y-4 text-sm text-slate-300">
        <h2 className="font-semibold text-white">수집하는 정보</h2>
        <p>본 서비스는 기본 서비스 이용을 위해 별도의 회원가입이나 개인정보를 수집하지 않습니다.</p>
        <h2 className="font-semibold text-white">자동 수집 정보</h2>
        <p>서비스 품질 개선을 위해 접속 로그(IP, 브라우저 정보)가 자동 수집될 수 있습니다.</p>
        <h2 className="font-semibold text-white">쿠키 및 로컬 스토리지</h2>
        <p>번호 저장 및 설정 유지를 위해 브라우저 로컬 스토리지를 사용합니다.</p>
        <h2 className="font-semibold text-white">문의</h2>
        <p>개인정보 관련 문의는 <a href="/info/contact" className="text-gold underline">문의하기</a>를 통해 주세요.</p>
      </div>
    ),
  },
  terms: {
    title: '이용약관',
    content: (
      <div className="space-y-4 text-sm text-slate-300">
        <p>KRAFT Lotto(이하 &quot;서비스&quot;)는 로또 번호 분석 도구로, 당첨을 보장하지 않습니다.</p>
        <h2 className="font-semibold text-white">면책 조항</h2>
        <p>본 서비스는 통계 참고 도구입니다. 서비스 이용으로 발생한 손실에 대해 책임지지 않습니다.</p>
        <h2 className="font-semibold text-white">저작권</h2>
        <p>당첨 데이터는 동행복권 공식 발표 기준이며, 서비스 코드 및 디자인 저작권은 운영자에게 있습니다.</p>
      </div>
    ),
  },
  contact: {
    title: '문의하기',
    content: (
      <div className="space-y-3 text-sm text-slate-300">
        <p>서비스 관련 문의, 버그 제보, 개선 제안은 아래로 연락해 주세요.</p>
        <p>이메일: <span className="text-gold">contact@kraft.io.kr</span></p>
      </div>
    ),
  },
  methodology: {
    title: '분석 방법론',
    content: (
      <div className="space-y-4 text-sm text-slate-300">
        <p>번호 추천은 과거 당첨 이력을 바탕으로 흔한 조합 패턴을 회피하는 방식으로 동작합니다.</p>
        <p>모든 로또 조합의 이론적 당첨 확률은 동일(1/8,145,060)합니다.</p>
        <p>출현 빈도, 홀짝 분포, 합산 범위는 과거 데이터의 통계적 요약입니다.</p>
      </div>
    ),
  },
  'data-source': {
    title: '데이터 출처',
    content: null, // DataSourcePage (client component) 로 렌더링
  },
  'responsible-play': {
    title: '책임감 있는 플레이',
    content: (
      <div className="space-y-4 text-sm text-slate-300">
        <p>로또는 오락 목적의 게임입니다. 과도한 구매는 경제적 어려움을 초래할 수 있습니다.</p>
        <p>도박 문제 상담: <span className="text-gold">한국도박문제관리센터 1336</span></p>
        <p>본 서비스는 책임감 있는 플레이를 권장합니다.</p>
      </div>
    ),
  },
}

export function generateStaticParams() {
  return Object.keys(STATIC_PAGES).map((slug) => ({ slug }))
}

export default function InfoPage({ params }: { params: { slug: string } }) {
  const page = STATIC_PAGES[params.slug]
  if (!page) notFound()

  return (
    <div className="space-y-6">
      <header className="space-y-1">
        <p className="eyebrow">안내</p>
        <h1 className="text-2xl font-bold">{page.title}</h1>
      </header>
      <AdSlot slotId={`info-${params.slug}-top`} />
      <div className="card">
        {params.slug === 'data-source' ? <DataSourcePage /> : page.content}
      </div>
    </div>
  )
}
