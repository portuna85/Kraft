export default function Loading() {
  return (
    <section className="panel">
      <div className="skeleton-line skeleton-eyebrow" />
      <div className="skeleton-line skeleton-h1" style={{ marginTop: "10px" }} />
      <div className="skeleton-line skeleton-body" style={{ marginTop: "14px", width: "35%" }} />
      <div className="skeleton-balls" style={{ marginTop: "24px" }}>
        {Array.from({ length: 7 }).map((_, i) => (
          <div key={i} className="skeleton-ball" />
        ))}
      </div>
      <div className="round-detail-grid">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="round-detail-cell">
            <div className="skeleton-line skeleton-caption" style={{ width: "80px" }} />
            <div className="skeleton-line skeleton-body" style={{ marginTop: "6px", width: "140px" }} />
          </div>
        ))}
      </div>
      <div style={{ marginTop: "24px" }}>
        <div className="skeleton-line skeleton-btn" />
      </div>
    </section>
  );
}
