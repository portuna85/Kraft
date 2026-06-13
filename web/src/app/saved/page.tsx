import type { Metadata } from "next";
import { SavedNumbersClient } from "@/components/saved-numbers-client";

export const metadata: Metadata = {
  title: "저장함",
  description: "브라우저 기준으로 저장한 로또 번호를 확인하고 삭제합니다.",
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
      <p className="eyebrow">저장 번호</p>
      <h1 className="page-title">저장함</h1>
      <p className="page-subtitle">저장된 번호는 이 브라우저의 기기 토큰과 연결됩니다.</p>
      <SavedNumbersClient />
    </section>
  );
}
