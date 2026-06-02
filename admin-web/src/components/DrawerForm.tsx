import type { ReactNode } from 'react';
import { X } from 'lucide-react';

export function DrawerForm({
  open,
  title,
  description,
  children,
  footer,
  onClose,
}: {
  open: boolean;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  onClose: () => void;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/35">
      <button aria-label="Đóng" className="hidden flex-1 cursor-default lg:block" onClick={onClose} type="button" />
      <aside className="flex h-full w-full max-w-xl flex-col border-l border-border bg-background shadow-float">
        <header className="flex min-h-topbar items-center justify-between border-b border-border bg-surface px-5">
          <div className="min-w-0">
            <h2 className="truncate text-lg font-extrabold text-text">{title}</h2>
            {description ? <p className="truncate text-sm text-muted">{description}</p> : null}
          </div>
          <button className="icon-btn" onClick={onClose} type="button">
            <X className="h-4 w-4" />
          </button>
        </header>
        <div className="flex-1 overflow-y-auto p-5">{children}</div>
        {footer ? <footer className="border-t border-border bg-surface px-5 py-4">{footer}</footer> : null}
      </aside>
    </div>
  );
}
