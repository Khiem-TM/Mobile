import type { ReactNode } from 'react';

type Variant = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const classes: Record<Variant, string> = {
  success: 'border-primary/25 bg-primary-soft text-primary-dark',
  warning: 'border-warning/25 bg-warning-soft text-warning',
  danger: 'border-danger/25 bg-danger-soft text-danger',
  info: 'border-info/25 bg-info-soft text-info',
  neutral: 'border-border bg-surface-high text-muted',
};

export function StatusBadge({ children, variant = 'neutral' }: { children: ReactNode; variant?: Variant }) {
  return (
    <span className={`inline-flex items-center gap-1 rounded px-2 py-1 text-xs font-bold ${classes[variant]}`}>
      <span className="h-1.5 w-1.5 rounded-full bg-current" />
      {children}
    </span>
  );
}

export function statusVariant(status?: string | boolean | null): Variant {
  if (status === true) return 'success';
  if (status === false) return 'danger';
  const normalized = String(status ?? '').toLowerCase();
  if (['ok', 'healthy', 'active', 'approved', 'success', 'configured'].includes(normalized)) return 'success';
  if (['pending', 'warning', 'degraded'].includes(normalized)) return 'warning';
  if (['error', 'inactive', 'rejected', 'disabled', 'missing_config'].includes(normalized)) return 'danger';
  if (['draft', 'info'].includes(normalized)) return 'info';
  return 'neutral';
}
