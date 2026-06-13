import type { Metadata } from "next";
import { notFound } from "next/navigation";

export const dynamic = "force-static";

type Props = { params: Promise<{ slug: string }> };

const infoPages: Record<string, { title: string; description: string }> = {
  "data-source": { title: "데이터 출처", description: "KRAFT Lotto 데이터 출처 안내입니다." },
  methodology: { title: "분석 방법론", description: "KRAFT Lotto 번호 분석 방법론을 설명합니다." },
  faq: { title: "자주 묻는 질문", description: "KRAFT Lotto 자주 묻는 질문입니다." },
  privacy: { title: "개인정보처리방침", description: "KRAFT Lotto 개인정보처리방침입니다." },
  terms: { title: "이용약관", description: "KRAFT Lotto 이용약관입니다." },
  contact: { title: "문의하기", description: "KRAFT Lotto 문의 안내입니다." },
  "responsible-play": { title: "건전한 이용", description: "건전한 로또 이용 안내입니다." }
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
    alternates: { canonical: `/info/${slug}` }
  };
}

export default async function InfoPage({ params }: Props) {
  const { slug } = await params;
  const info = infoPages[slug];
  if (!info) notFound();

  return (
    <section className="panel">
      <h1 className="page-title">{info.title}</h1>
      <p className="page-subtitle">{info.description}</p>
    </section>
  );
}
