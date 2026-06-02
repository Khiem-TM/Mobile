import { Bell, Grid3X3, Menu, Search, Settings2 } from 'lucide-react';
import { useAuth } from '../lib/auth';
import { getUserDisplayName } from '../lib/format';

export function Topbar({ onOpenSidebar }: { onOpenSidebar: () => void }) {
  const { session } = useAuth();
  const userName = getUserDisplayName(session?.user);
  return (
    <header className="sticky top-0 z-30 flex h-topbar items-center gap-3 border-b border-border bg-background/95 px-4 backdrop-blur lg:px-6">
      <button className="icon-btn lg:hidden" onClick={onOpenSidebar} type="button">
        <Menu className="h-5 w-5" />
      </button>
      <div className="relative mx-auto hidden w-full max-w-2xl md:block">
        <Search className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted" />
        <input className="input h-11 rounded-full bg-surface-low pl-12" placeholder="Tìm kiếm tài nguyên, người dùng..." />
      </div>
      <div className="ml-auto flex items-center gap-1">
        <button className="icon-btn relative" type="button">
          <Bell className="h-5 w-5" />
          <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-danger" />
        </button>
        <button className="icon-btn" type="button">
          <Settings2 className="h-5 w-5" />
        </button>
        <button className="icon-btn" type="button">
          <Grid3X3 className="h-5 w-5" />
        </button>
        <div className="ml-2 hidden h-8 w-px bg-border sm:block" />
        <div className="ml-2 flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-sm font-extrabold text-white">
            {userName.slice(0, 1).toUpperCase()}
          </div>
          <div className="hidden min-w-0 sm:block">
            <p className="max-w-36 truncate text-sm font-extrabold text-text">{userName}</p>
            <p className="text-xs font-semibold text-muted">Admin</p>
          </div>
        </div>
      </div>
    </header>
  );
}
