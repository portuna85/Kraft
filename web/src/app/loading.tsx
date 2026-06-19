export default function Loading() {
  return (
    <div className="grid">
      <section className="panel result-panel" style={{ marginBottom: "24px" }}>
        <div className="skeleton-line skeleton-eyebrow" />
        <div className="skeleton-line skeleton-h2" style={{ marginTop: "10px" }} />
        <div className="skeleton-line skeleton-body" style={{ marginTop: "6px", width: "60%" }} />
        <div className="skeleton-balls" style={{ marginTop: "14px" }}>
          {Array.from({ length: 7 }).map((_, i) => (
            <div key={i} className="skeleton-ball" />
          ))}
        </div>
        <div className="skeleton-line skeleton-body" style={{ marginTop: "16px", height: "84px" }} />
      </section>
      <section className="grid grid-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="stat-card">
            <div className="skeleton-line skeleton-eyebrow" />
            <div className="skeleton-line skeleton-body" style={{ marginTop: "8px", width: "80%" }} />
            <div className="skeleton-line skeleton-body" style={{ marginTop: "6px" }} />
            <div className="skeleton-line skeleton-body" style={{ marginTop: "4px", width: "70%" }} />
          </div>
        ))}
      </section>
    </div>
  );
}
