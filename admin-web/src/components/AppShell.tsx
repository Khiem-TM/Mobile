import { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

export function AppShell() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  return (
    <div className="min-h-screen bg-background text-text">
      <div className="fixed inset-y-0 left-0 z-40 hidden w-sidebar lg:block">
        <Sidebar />
      </div>
      {sidebarOpen ? (
        <div className="fixed inset-0 z-50 flex bg-black/35 lg:hidden">
          <Sidebar onNavigate={() => setSidebarOpen(false)} />
          <button aria-label="Đóng menu" className="flex-1" onClick={() => setSidebarOpen(false)} type="button" />
        </div>
      ) : null}
      <div className="min-h-screen lg:pl-sidebar">
        <Topbar onOpenSidebar={() => setSidebarOpen(true)} />
        <main className="mx-auto w-full max-w-[1600px] p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
