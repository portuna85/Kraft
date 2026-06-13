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
      <h1 className="page-title">잠시 후 다시 시도해 주세요</h1>
      <p className="page-subtitle">
        데이터를 불러오는 중 일시적인 오류가 발생했습니다.
        {error.digest ? ` (${error.digest})` : ""}
      </p>
      <div style={{ marginTop: "24px" }}>
        <button onClick={reset}>다시 시도</button>
      </div>
    </section>
  );
}
