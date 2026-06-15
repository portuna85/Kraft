import type { Metadata } from "next";
import { RecommendClient } from "@/components/recommend-client";

export const metadata: Metadata = {
  title: "번호 추천",
  description: "제외 번호를 반영해 추천 조합을 만들고 저장할 수 있습니다.",
  alternates: {
    canonical: "/recommend",
  },
};

export default function RecommendPage() {
  return (
    <section className="panel">
      <p className="eyebrow">번호 추천</p>
      <h1 className="page-title">번호 추천</h1>
      <RecommendClient />
    </section>
  );
}
