"use client";

import Link from "next/link";
import { useEffect } from "react";

export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <section className="panel not-found">
      <p className="eyebrow">오류 발생</p>
      <h1 className="page-title">페이지를 불러오지 못했습니다</h1>
      <p className="page-subtitle">
        데이터를 가져오는 중 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.
        {error.digest ? ` (코드: ${error.digest})` : ""}
      </p>
      <div className="not-found-actions">
        <button onClick={reset}>다시 불러오기</button>
        <Link href="/" className="button secondary">홈으로 이동</Link>
      </div>
    </section>
  );
}
