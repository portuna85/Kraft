import type { Metadata } from "next";
import { RecommendClient } from "@/components/recommend-client";

export const metadata: Metadata = {
  title: "번호 추천",
  description: "제외 번호를 반영해 로또 추천 조합을 만들고 저장함에 보관할 수 있습니다.",
  alternates: {
    canonical: "/recommend"
  }
};

export default function RecommendPage() {
  return (
    <section className="panel">
      <p className="eyebrow">번호 추천</p>
      <h1 className="page-title">추천 조합 만들기</h1>
      <p className="page-subtitle">원하지 않는 번호를 제외하고 여러 조합을 만든 뒤, 마음에 드는 번호만 저장해 두세요.</p>
      <RecommendClient />
    </section>
  );
}
