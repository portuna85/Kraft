import { RecommendClient } from "@/components/recommend-client";

export const metadata = {
  title: "번호 추천",
  description: "제외 번호를 반영해 로또 추천 번호를 생성하고 바로 저장합니다.",
  alternates: {
    canonical: "/recommend"
  }
};

export default function RecommendPage() {
  return (
    <section className="panel">
      <p className="eyebrow">번호 추천</p>
      <h1 className="page-title">추천 번호 생성</h1>
      <p className="page-subtitle">제외 번호를 지정하고, 생성된 추천 번호를 저장함으로 바로 보낼 수 있습니다.</p>
      <RecommendClient />
    </section>
  );
}
