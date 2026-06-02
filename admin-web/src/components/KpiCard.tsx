import type { ReactNode } from 'react';
import { TrendingDown, TrendingUp } from 'lucide-react';

interface KpiCardProps {
  label: string;
  value: string;
  hint?: string;
  trend?: string;
  tone?: 'green' | 'amber' | 'red' | 'blue';
  icon?: ReactNode;
}

const toneClasses = {
  green: 'bg-primary-soft text-primary',
  amber: 'bg-warning-soft text-warning',
  red: 'bg-danger-soft text-danger',
  blue: 'bg-info-soft text-info',
};

export function KpiCard({ label, value, hint, trend, tone = 'green', icon }: KpiCardProps) {
  const trendIsDown = trend?.trim().startsWith('-');
  return (
    <div className="card min-h-[132px] p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="label line-clamp-2">{label}</p>
          <p className="mt-6 text-3xl font-extrabold tracking-normal text-text">{value}</p>
        </div>
        <div className={`flex h-10 min-w-10 items-center justify-center rounded-lg ${toneClasses[tone]}`}>{icon}</div>
      </div>
      <div className="mt-3 flex min-h-6 items-center justify-between gap-2 text-sm text-muted">
        <span className="truncate">{hint}</span>
        {trend ? (
          <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-primary-soft px-2 py-1 text-xs font-bold text-primary">
            {trendIsDown ? <TrendingDown className="h-3 w-3" /> : <TrendingUp className="h-3 w-3" />}
            {trend}
          </span>
        ) : null}
      </div>
    </div>
  );
}
