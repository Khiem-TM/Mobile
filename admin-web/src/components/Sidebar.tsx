import { NavLink } from 'react-router-dom';
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Dumbbell,
  FileText,
  History,
  LogOut,
  Plus,
  Settings,
  ShieldQuestion,
  Utensils,
  Users,
  LayoutDashboard,
} from 'lucide-react';
import { useAuth } from '../lib/auth';

const navItems = [
  { label: 'Tổng quan', path: '/', icon: LayoutDashboard },
  { label: 'Người dùng', path: '/users', icon: Users },
  { label: 'Thực phẩm', path: '/foods', icon: Utensils },
  { label: 'Bài tập', path: '/exercises', icon: Dumbbell },
  { label: 'Blog & CMS', path: '/blogs', icon: FileText },
  { label: 'Cảnh báo', path: '/warnings', icon: AlertTriangle },
  { label: 'Phân tích', path: '/analytics', icon: BarChart3 },
  { label: 'Hệ thống', path: '/system', icon: Settings },
  { label: 'Nhật ký hệ thống', path: '/audit', icon: History },
];

export function Sidebar({ onNavigate }: { onNavigate?: () => void }) {
  const { logout } = useAuth();
  return (
    <aside className="flex h-full w-sidebar flex-col bg-primary text-white">
      <div className="flex items-center gap-3 px-7 py-8">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg border border-white/25 bg-white/10">
          <Activity className="h-5 w-5" />
        </div>
        <div className="min-w-0">
          <h1 className="truncate text-xl font-extrabold tracking-normal">Calories Tracker</h1>
          <p className="text-xs font-extrabold uppercase tracking-wide text-primary-soft">Hệ thống quản trị</p>
        </div>
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto px-4">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-r-full px-5 py-3 text-[15px] font-bold transition ${
                  isActive
                    ? 'bg-white text-primary shadow-soft'
                    : 'text-primary-soft hover:bg-white/10 hover:text-white'
                }`
              }
              end={item.path === '/'}
              key={item.path}
              onClick={onNavigate}
              to={item.path}
            >
              <Icon className="h-5 w-5 shrink-0" />
              <span className="truncate">{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <div className="border-t border-white/20 p-4">
        <button className="mb-4 flex h-12 w-full items-center justify-center gap-2 rounded-lg border border-white/25 bg-white/10 text-sm font-extrabold text-white hover:bg-white/15" type="button">
          <Plus className="h-4 w-4" />
          Tạo báo cáo
        </button>
        <div className="space-y-1">
          <button className="flex w-full items-center gap-3 rounded-md px-4 py-2.5 text-sm font-bold text-primary-soft hover:bg-white/10 hover:text-white" type="button">
            <ShieldQuestion className="h-4 w-4" />
            Hỗ trợ
          </button>
          <button
            className="flex w-full items-center gap-3 rounded-md px-4 py-2.5 text-sm font-bold text-primary-soft hover:bg-white/10 hover:text-white"
            onClick={logout}
            type="button"
          >
            <LogOut className="h-4 w-4" />
            Đăng xuất
          </button>
        </div>
      </div>
    </aside>
  );
}
