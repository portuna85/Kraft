export default function Loading() {
  return (
    <section className="panel">
      <div className="skeleton-line skeleton-eyebrow" />
      <div className="skeleton-line skeleton-h1" style={{ marginTop: "10px" }} />
      <div className="skeleton-line skeleton-body" style={{ marginTop: "14px", width: "30%" }} />
      <div className="skeleton-balls" style={{ marginTop: "18px" }}>
        {Array.from({ length: 7 }).map((_, i) => (
          <div key={i} className="skeleton-ball" />
        ))}
      </div>
      <div className="skeleton-line skeleton-body" style={{ marginTop: "18px", width: "40%" }} />
    </section>
  );
}
