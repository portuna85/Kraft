export default function Loading() {
  return (
    <section className="panel">
      <div className="skeleton-line skeleton-eyebrow" />
      <div className="skeleton-line skeleton-h1" style={{ marginTop: "10px" }} />
      <div className="skeleton-line skeleton-body" style={{ marginTop: "14px", width: "25%" }} />
      <div className="round-list" style={{ marginTop: "24px" }}>
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="round-card skeleton-round-card">
            <div className="round-meta">
              <div style={{ display: "grid", gap: "8px" }}>
                <div className="skeleton-line skeleton-body" style={{ width: "80px" }} />
                <div className="skeleton-line skeleton-caption" style={{ width: "120px" }} />
              </div>
              <div className="skeleton-line skeleton-btn" />
            </div>
            <div className="skeleton-balls">
              {Array.from({ length: 7 }).map((_, j) => (
                <div key={j} className="skeleton-ball" />
              ))}
            </div>
            <div className="skeleton-line skeleton-caption" style={{ marginTop: "8px", width: "160px" }} />
          </div>
        ))}
      </div>
    </section>
  );
}
