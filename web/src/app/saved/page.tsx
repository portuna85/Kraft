import type { Metadata } from "next";
import { SavedNumbersClient } from "@/components/saved-numbers-client";

export const metadata: Metadata = {
  title: "저장함",
  description: "이 브라우저에 연결된 저장 번호를 확인하고 정리할 수 있습니다.",
  robots: {
    index: false,
    follow: false
  },
  alternates: {
    canonical: "/saved"
  }
};

export default function SavedPage() {
  return (
    <section className="panel">
      <p className="eyebrow">내 번호</p>
      <h1 className="page-title">저장한 번호 모음</h1>
      <p className="page-subtitle">저장한 조합은 현재 브라우저의 익명 기기 토큰과 연결되어 관리됩니다.</p>
      <SavedNumbersClient />
    </section>
  );
}
