import { Inbox } from 'lucide-react';

export function EmptyState({ title = 'Không có dữ liệu', description }: { title?: string; description?: string }) {
  return (
    <div className="flex min-h-36 flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-border bg-surface-low p-6 text-center">
      <Inbox className="h-6 w-6 text-muted" />
      <p className="text-sm font-bold text-text">{title}</p>
      {description ? <p className="max-w-lg text-sm text-muted">{description}</p> : null}
    </div>
  );
}
