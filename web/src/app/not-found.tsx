import Link from "next/link";

export default function NotFound() {
  return (
    <section className="panel">
      <p className="eyebrow">404</p>
      <h1 className="page-title">요청한 페이지를 찾을 수 없습니다.</h1>
      <p className="page-subtitle">주소를 다시 확인하거나 최신 회차 페이지로 이동하세요.</p>
      <Link href="/latest" className="button">
        최신 회차로 이동
      </Link>
    </section>
  );
}
