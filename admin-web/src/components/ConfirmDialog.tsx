import type { ReactNode } from 'react';
import { AlertTriangle, X } from 'lucide-react';

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Xác nhận',
  tone = 'danger',
  isLoading,
  children,
  onCancel,
  onConfirm,
}: {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  tone?: 'danger' | 'primary';
  isLoading?: boolean;
  children?: ReactNode;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 p-4">
      <div className="w-full max-w-md rounded-lg border border-border bg-surface p-5 shadow-float">
        <div className="flex items-start justify-between gap-4">
          <div className="flex gap-3">
            <div className={tone === 'danger' ? 'text-danger' : 'text-primary'}>
              <AlertTriangle className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-text">{title}</h2>
              {description ? <p className="mt-1 text-sm text-muted">{description}</p> : null}
            </div>
          </div>
          <button className="icon-btn -mr-2 -mt-2" onClick={onCancel} type="button">
            <X className="h-4 w-4" />
          </button>
        </div>
        {children ? <div className="mt-4">{children}</div> : null}
        <div className="mt-5 flex justify-end gap-2">
          <button className="btn-secondary" disabled={isLoading} onClick={onCancel} type="button">
            Hủy
          </button>
          <button
            className={tone === 'danger' ? 'btn-danger' : 'btn-primary'}
            disabled={isLoading}
            onClick={onConfirm}
            type="button"
          >
            {isLoading ? 'Đang xử lý' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
