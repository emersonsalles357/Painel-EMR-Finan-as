import { formatCurrency } from '../utils';

interface MetricCardProps {
  label: string;
  value: number;
  icon: string;
  hint?: string;
}

export function MetricCard({ label, value, icon, hint }: MetricCardProps) {
  return (
    <div className="metric-card">
      <div className="metric-icon"><i className={`bi ${icon}`}></i></div>
      <div className="metric-value">{formatCurrency(value)}</div>
      <div className="metric-label">{label}</div>
      {hint && <div className="small text-muted-soft mt-3"><i className="bi bi-graph-up-arrow me-1"></i>{hint}</div>}
    </div>
  );
}
