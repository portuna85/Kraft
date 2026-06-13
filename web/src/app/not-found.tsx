import Link from "next/link";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "페이지를 찾을 수 없습니다",
  robots: { index: false, follow: false },
};

export default function NotFound() {
  return (
    <section className="panel not-found">
      <p className="eyebrow">오류 404</p>
      <h1 className="page-title not-found-title">페이지를<br />찾을 수 없습니다</h1>
      <p className="page-subtitle">
        요청하신 주소가 존재하지 않거나 이동되었습니다.
      </p>
      <div className="not-found-actions">
        <Link href="/" className="button">홈으로</Link>
        <Link href="/latest" className="button secondary">최신 회차</Link>
        <Link href="/rounds" className="button secondary">회차 목록</Link>
      </div>
    </section>
  );
}
