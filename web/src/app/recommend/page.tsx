import type { Metadata } from "next";
import { RecommendClient } from "@/components/recommend-client";

export const metadata: Metadata = {
  title: "번호 추천",
  description: "1~45에서 제외할 번호를 설정하면 6개를 무작위로 추천합니다. 추천 조합을 저장하고 관리할 수 있습니다.",
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
