import { AlertTriangle, RefreshCw } from 'lucide-react';

export function ErrorState({ message, onRetry }: { message?: string; onRetry?: () => void }) {
  return (
    <div className="flex min-h-40 flex-col items-center justify-center gap-3 rounded-lg border border-danger/25 bg-danger-soft/35 p-6 text-center">
      <AlertTriangle className="h-6 w-6 text-danger" />
      <p className="max-w-xl text-sm font-semibold text-danger">{message || 'Không thể tải dữ liệu'}</p>
      {onRetry ? (
        <button className="btn-secondary h-9" onClick={onRetry} type="button">
          <RefreshCw className="h-4 w-4" />
          Tải lại
        </button>
      ) : null}
    </div>
  );
}
