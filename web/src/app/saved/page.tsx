import type { Metadata } from "next";
import { SavedNumbersClient } from "@/components/saved-numbers-client";

export const metadata: Metadata = {
  title: "저장한 번호",
  description: "이 브라우저에 저장한 번호를 확인하고 정리할 수 있습니다.",
  robots: {
    index: false,
    follow: false,
  },
  alternates: {
    canonical: "/saved",
  },
};

export default function SavedPage() {
  return (
    <section className="panel">
      <p className="eyebrow">저장한 번호</p>
      <h1 className="page-title">저장한 번호 모음</h1>
      <p className="page-subtitle">저장한 조합을 다시 보고 정리할 수 있습니다.</p>
      <SavedNumbersClient />
    </section>
  );
}
