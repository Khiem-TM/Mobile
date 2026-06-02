import { History, RotateCcw } from 'lucide-react';
import { EmptyState } from '../components/EmptyState';
import { FilterBar } from '../components/FilterBar';

export function AuditPage() {
  return (
    <div className="space-y-5">
      <div>
        <h1 className="page-title">Nhật ký hệ thống</h1>
        <p className="mt-1 text-sm text-muted">Theo dõi audit log cho thao tác admin</p>
      </div>

      <FilterBar
        actions={
          <button className="btn-secondary" disabled type="button">
            <RotateCcw className="h-4 w-4" />
            Đặt lại
          </button>
        }
      >
        <input className="input" disabled placeholder="Actor email" />
        <input className="input" disabled placeholder="Action" />
        <input className="input" disabled placeholder="Target type" />
        <select className="select" disabled>
          <option>Status</option>
        </select>
        <input className="input" disabled type="date" />
      </FilterBar>

      <section className="card min-h-[420px] p-5">
        <div className="mb-4 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-faint text-primary">
            <History className="h-5 w-5" />
          </div>
          <div>
            <h2 className="text-lg font-extrabold text-text">Audit logs</h2>
            <p className="text-sm text-muted">API chưa có endpoint danh sách audit logs</p>
          </div>
        </div>
        <EmptyState title="API chưa có endpoint danh sách audit logs" description="UI table shell và filter đã được chuẩn bị cho GET /admin/audit-logs." />
      </section>
    </div>
  );
}
