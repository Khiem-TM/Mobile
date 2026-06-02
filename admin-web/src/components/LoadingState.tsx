export function LoadingState({ label = 'Đang tải dữ liệu' }: { label?: string }) {
  return (
    <div className="flex min-h-40 items-center justify-center rounded-lg border border-dashed border-border bg-surface-low">
      <div className="flex items-center gap-3 text-sm font-semibold text-muted">
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-primary/20 border-t-primary" />
        {label}
      </div>
    </div>
  );
}
