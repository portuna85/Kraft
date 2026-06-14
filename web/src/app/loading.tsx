export default function Loading() {
  return (
    <div className="grid">
      <section className="hero">
        <div>
          <div className="skeleton-line skeleton-eyebrow" />
          <div className="skeleton-line skeleton-h1" style={{ marginTop: "12px" }} />
          <div className="skeleton-line skeleton-h1" style={{ width: "70%", marginTop: "8px" }} />
          <div className="skeleton-line skeleton-body" style={{ marginTop: "16px" }} />
          <div className="skeleton-actions" style={{ marginTop: "26px" }}>
            <div className="skeleton-line skeleton-btn" />
            <div className="skeleton-line skeleton-btn" />
            <div className="skeleton-line skeleton-btn" />
          </div>
        </div>
        <aside className="hero-side">
          <div className="skeleton-line skeleton-eyebrow" />
          <div className="skeleton-line skeleton-h2" style={{ marginTop: "10px" }} />
          <div className="skeleton-line skeleton-body" style={{ marginTop: "6px", width: "60%" }} />
          <div className="skeleton-balls" style={{ marginTop: "14px" }}>
            {Array.from({ length: 7 }).map((_, i) => (
              <div key={i} className="skeleton-ball" />
            ))}
          </div>
        </aside>
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
