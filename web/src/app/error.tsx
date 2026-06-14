"use client";

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
    <section className="panel">
      <p className="eyebrow">오류</p>
      <h1 className="page-title">페이지를 불러오지 못했습니다</h1>
      <p className="page-subtitle">
        데이터를 가져오는 중 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.
        {error.digest ? ` (${error.digest})` : ""}
      </p>
      <div style={{ marginTop: "24px" }}>
        <button onClick={reset}>다시 불러오기</button>
      </div>
    </section>
  );
}
