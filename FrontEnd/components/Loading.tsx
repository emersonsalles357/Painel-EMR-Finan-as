export function LoadingSkeleton() {
  return (
    <div className="page-enter">
      <div className="row g-3 mb-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="col-md-3"><div className="skeleton" style={{ height: '150px' }}></div></div>
        ))}
      </div>
      <div className="skeleton" style={{ height: '360px' }}></div>
    </div>
  );
}
