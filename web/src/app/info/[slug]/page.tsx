import type { Metadata } from "next";
import { notFound } from "next/navigation";

export const dynamic = "force-static";

type Props = { params: Promise<{ slug: string }> };

type InfoPage = {
  title: string;
  description: string;
  content: React.ReactNode;
};

const infoPages: Record<string, InfoPage> = {
  "data-source": {
    title: "데이터 출처",
    description: "KRAFT Lotto 데이터 출처 안내입니다.",
    content: (
      <article className="info-article">
        <h2>공식 데이터 출처</h2>
        <p>
          KRAFT Lotto에 표시되는 모든 로또 6/45 당첨 번호 및 당첨 정보는
          동행복권 공식 웹사이트(<strong>dhlottery.co.kr</strong>)에서
          제공하는 공개 API를 통해 수집됩니다.
        </p>

        <h2>수집 방식</h2>
        <ul>
          <li>매 회차 추첨 후 자동으로 최신 결과를 수집합니다.</li>
          <li>수집된 데이터는 내부 데이터베이스에 저장되며, 서비스 안정성을 위해 캐싱됩니다.</li>
          <li>1회차부터 최신 회차까지 전체 이력을 보유하고 있습니다.</li>
        </ul>

        <h2>데이터 정확성</h2>
        <p>
          당첨 번호·당첨금·판매금액 등 모든 수치는 동행복권 공식 발표 기준입니다.
          통계 분석 결과는 수집된 과거 데이터를 기반으로 계산되며,
          미래 당첨 번호를 예측하거나 보장하지 않습니다.
        </p>

        <h2>저작권</h2>
        <p>
          동행복권이 공개한 당첨 번호 데이터는 공공 정보로 제공됩니다.
          KRAFT Lotto는 해당 데이터를 비상업적 분석 목적으로 활용합니다.
        </p>
      </article>
    ),
  },

  methodology: {
    title: "분석 방법론",
    description: "KRAFT Lotto 번호 분석 방법론을 설명합니다.",
    content: (
      <article className="info-article">
        <h2>통계 분석 개요</h2>
        <p>
          KRAFT Lotto의 통계 기능은 1회차부터 최신 회차까지의 실제 당첨 번호를
          기반으로 순수 통계 빈도를 계산합니다. 어떤 예측 모델이나 머신러닝도
          사용하지 않습니다.
        </p>

        <h2>빈도 분석 (Frequency)</h2>
        <p>
          각 번호(1~45)가 전체 회차에서 당첨 번호(보너스 제외)로 등장한 횟수를 집계합니다.
          상위/하위 5개 번호를 강조 표시하여 상대적인 출현 빈도를 파악할 수 있습니다.
        </p>

        <h2>패턴 분석 (Pattern)</h2>
        <ul>
          <li><strong>홀짝 비율:</strong> 당첨 번호 6개 중 홀수 개수를 0~6으로 분류합니다.</li>
          <li><strong>고저 비율:</strong> 1~22를 저번호, 23~45를 고번호로 분류하여 고번호 개수를 집계합니다.</li>
          <li><strong>합계 구간:</strong> 6개 번호의 합계를 50단위 구간(~100, 101~150, …)으로 나눕니다.</li>
        </ul>

        <h2>동반 분석 (Companion)</h2>
        <p>
          두 번호가 같은 회차에 함께 등장한 횟수를 모든 조합(45C2 = 990쌍)에 대해 계산하고,
          공동 출현 횟수 기준으로 상위 쌍을 표시합니다.
        </p>

        <h2>번호 추천</h2>
        <p>
          서버측 보안 난수 생성기(SecureRandom)를 사용하여 1~45에서 중복 없이
          6개를 무작위 추출합니다. 과거 통계와 무관한 완전 무작위 방식이며,
          제외 번호를 지정하면 해당 번호를 후보에서 제거한 후 추출합니다.
        </p>

        <h2>중요 안내</h2>
        <p>
          로또는 매 회차 독립 시행입니다. 과거 통계가 미래 결과에 영향을 주지 않으며,
          어떤 번호 조합도 다른 조합보다 당첨 확률이 높거나 낮지 않습니다.
        </p>
      </article>
    ),
  },

  faq: {
    title: "자주 묻는 질문",
    description: "KRAFT Lotto 자주 묻는 질문입니다.",
    content: (
      <article className="info-article">
        <div className="faq-item">
          <h3>추천 번호로 사면 당첨 확률이 높아지나요?</h3>
          <p>
            아니요. KRAFT Lotto의 번호 추천은 완전 무작위 방식이며,
            통계적으로 특정 번호 조합이 다른 조합보다 유리하지 않습니다.
            로또 6/45의 당첨 확률은 조합 방법과 무관하게 동일합니다(약 1/8,145,060).
          </p>
        </div>

        <div className="faq-item">
          <h3>저장함은 어디에 저장되나요?</h3>
          <p>
            저장함은 브라우저에 자동 생성되는 익명 기기 토큰과 연결되어
            서버에 저장됩니다. 브라우저 데이터를 삭제하거나 다른 기기에서
            접속하면 저장 목록에 접근할 수 없습니다.
          </p>
        </div>

        <div className="faq-item">
          <h3>당첨 번호가 얼마나 빨리 업데이트되나요?</h3>
          <p>
            매주 토요일 추첨 후 자동으로 최신 회차를 수집합니다.
            보통 추첨 후 수 분 이내에 반영됩니다.
          </p>
        </div>

        <div className="faq-item">
          <h3>몇 회차부터 데이터가 있나요?</h3>
          <p>
            2002년 12월 7일 제1회부터 최신 회차까지 전체 이력을 보유하고 있습니다.
          </p>
        </div>

        <div className="faq-item">
          <h3>앱이 있나요?</h3>
          <p>
            현재는 웹 서비스만 제공합니다. 모바일 브라우저에서도 이용할 수 있습니다.
          </p>
        </div>

        <div className="faq-item">
          <h3>오류나 개선 요청은 어떻게 하나요?</h3>
          <p>
            문의하기 페이지를 이용해 주세요.
          </p>
        </div>
      </article>
    ),
  },

  privacy: {
    title: "개인정보처리방침",
    description: "KRAFT Lotto 개인정보처리방침입니다.",
    content: (
      <article className="info-article">
        <p className="muted">시행일: 2026년 1월 1일</p>

        <h2>수집하는 정보</h2>
        <p>
          KRAFT Lotto는 회원가입 없이 이용할 수 있습니다.
          저장함 기능 이용 시 브라우저에 익명 기기 토큰(UUID)이 생성되며,
          이 토큰과 연결된 저장 번호만 서버에 보관됩니다.
          성명·이메일·연락처 등 개인 식별 정보는 수집하지 않습니다.
        </p>

        <h2>정보 이용 목적</h2>
        <ul>
          <li>기기 토큰: 저장함 데이터를 기기와 연결하기 위한 익명 식별자로만 사용</li>
          <li>서버 로그: 서비스 장애 대응 및 보안 목적으로 IP 주소, 접근 시각을 단기 보관</li>
        </ul>

        <h2>제3자 제공</h2>
        <p>수집된 정보는 어떤 제3자에게도 제공·판매·공유되지 않습니다.</p>

        <h2>보관 기간</h2>
        <p>
          저장 번호는 사용자가 직접 삭제하거나 서비스 종료 시 파기됩니다.
          서버 로그는 최대 30일간 보관 후 삭제됩니다.
        </p>

        <h2>이용자의 권리</h2>
        <p>
          저장한 번호는 저장함 페이지에서 언제든 삭제할 수 있습니다.
          서버에 보관된 데이터 삭제를 요청하려면 문의하기를 이용하세요.
        </p>

        <h2>문의</h2>
        <p>개인정보 관련 문의는 문의하기 페이지를 이용하시기 바랍니다.</p>
      </article>
    ),
  },

  terms: {
    title: "이용약관",
    description: "KRAFT Lotto 이용약관입니다.",
    content: (
      <article className="info-article">
        <p className="muted">시행일: 2026년 1월 1일</p>

        <h2>제1조 목적</h2>
        <p>
          본 약관은 KRAFT Lotto(이하 "서비스")가 제공하는 로또 통계·번호 추천
          서비스의 이용 조건을 정합니다.
        </p>

        <h2>제2조 서비스 내용</h2>
        <p>
          서비스는 동행복권 공식 데이터를 기반으로 과거 당첨 번호 조회,
          통계 분석, 무작위 번호 추천 기능을 제공합니다.
          서비스의 어떤 기능도 당첨을 보장하거나 예측하지 않습니다.
        </p>

        <h2>제3조 이용자 의무</h2>
        <ul>
          <li>서비스를 상업적 목적으로 크롤링·스크레이핑하는 행위를 금지합니다.</li>
          <li>서비스의 안정적 운영을 방해하는 행위를 금지합니다.</li>
        </ul>

        <h2>제4조 면책 사항</h2>
        <p>
          서비스는 통계 정보 제공 목적으로만 운영되며, 번호 추천이나 통계 결과로
          인한 구매 결정 및 손실에 대해 책임을 지지 않습니다.
          서비스 중단, 데이터 오류 등으로 인한 손해에 대해서도 책임을 지지 않습니다.
        </p>

        <h2>제5조 약관 변경</h2>
        <p>
          약관 변경 시 서비스 내 공지를 통해 사전 안내합니다.
          변경 후 계속 이용하면 변경된 약관에 동의한 것으로 간주합니다.
        </p>
      </article>
    ),
  },

  contact: {
    title: "문의하기",
    description: "KRAFT Lotto 문의 안내입니다.",
    content: (
      <article className="info-article">
        <h2>문의 방법</h2>
        <p>
          서비스 이용 중 오류, 데이터 오류 신고, 개선 제안, 개인정보 삭제 요청 등
          모든 문의는 아래 이메일로 연락해 주세요.
        </p>

        <div className="contact-box">
          <p>
            <strong>이메일:</strong>{" "}
            <a href="mailto:portuna85@gmail.com">portuna85@gmail.com</a>
          </p>
          <p className="muted">
            문의 주시면 영업일 기준 2~3일 내 답변 드립니다.
          </p>
        </div>

        <h2>버그 신고 시 포함 사항</h2>
        <ul>
          <li>문제가 발생한 페이지 URL</li>
          <li>사용한 브라우저와 기기 종류</li>
          <li>오류 내용 및 재현 방법</li>
        </ul>

        <h2>데이터 오류 신고</h2>
        <p>
          특정 회차의 당첨 번호나 당첨금이 공식 발표와 다를 경우
          회차 번호와 함께 알려주시면 확인 후 수정하겠습니다.
        </p>
      </article>
    ),
  },

  "responsible-play": {
    title: "건전한 이용",
    description: "건전한 로또 이용 안내입니다.",
    content: (
      <article className="info-article">
        <div className="notice-box">
          <strong>로또는 오락을 위한 소액 게임입니다.</strong>
          <p>당첨 가능성을 과대평가하지 마시고, 여유 자금 범위 내에서 즐기세요.</p>
        </div>

        <h2>건전한 이용 원칙</h2>
        <ul>
          <li>생활비·대출금 등 필수 자금은 절대 사용하지 마세요.</li>
          <li>주 1회, 정해진 금액 이내로 구매 한도를 스스로 설정하세요.</li>
          <li>당첨을 목적으로 한 재정 계획은 세우지 마세요.</li>
          <li>손실을 만회하기 위해 추가 구매하지 마세요.</li>
        </ul>

        <h2>KRAFT Lotto의 역할</h2>
        <p>
          KRAFT Lotto는 과거 당첨 번호 통계를 제공할 뿐, 미래 당첨 번호를
          예측하거나 당첨을 보장하지 않습니다. 통계적으로 과거 결과는 미래
          결과에 영향을 미치지 않습니다.
        </p>

        <h2>도움이 필요하다면</h2>
        <p>
          도박 문제로 어려움을 겪고 계시다면 아래 기관에 도움을 요청하세요.
        </p>
        <ul>
          <li>
            <strong>한국도박문제관리센터:</strong>{" "}
            <a href="tel:1336">☎ 1336</a> (24시간)
          </li>
          <li>
            <strong>도박문제 무료 상담 온라인:</strong>{" "}
            <a
              href="https://www.kcgp.or.kr"
              target="_blank"
              rel="noopener noreferrer"
            >
              www.kcgp.or.kr
            </a>
          </li>
        </ul>
      </article>
    ),
  },
};

export function generateStaticParams() {
  return Object.keys(infoPages).map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug } = await params;
  const info = infoPages[slug];
  if (!info) return {};
  return {
    title: info.title,
    description: info.description,
    alternates: { canonical: `/info/${slug}` },
  };
}

export default async function InfoPage({ params }: Props) {
  const { slug } = await params;
  const info = infoPages[slug];
  if (!info) notFound();

  return (
    <section className="panel">
      <p className="eyebrow">안내</p>
      <h1 className="page-title">{info.title}</h1>
      {info.content}
    </section>
  );
}
