import type { ReactNode } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { EmptyState } from './EmptyState';
import { ErrorState } from './ErrorState';
import { LoadingState } from './LoadingState';
import { pageCount } from '../lib/format';

export interface DataColumn<T> {
  key: string;
  header: string;
  width?: string;
  className?: string;
  render: (row: T) => ReactNode;
}

interface DataTableProps<T> {
  data: T[];
  columns: DataColumn<T>[];
  getRowId: (row: T) => string;
  isLoading?: boolean;
  error?: string;
  emptyTitle?: string;
  page?: number;
  limit?: number;
  total?: number;
  onPageChange?: (page: number) => void;
}

export function DataTable<T>({
  data,
  columns,
  getRowId,
  isLoading,
  error,
  emptyTitle,
  page = 1,
  limit = 20,
  total = data.length,
  onPageChange,
}: DataTableProps<T>) {
  if (isLoading) return <LoadingState />;
  if (error) return <ErrorState message={error} />;
  if (!data.length) return <EmptyState title={emptyTitle} />;

  const pages = pageCount(total, limit);

  return (
    <div className="card overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full border-collapse text-sm">
          <thead className="bg-surface-low">
            <tr>
              {columns.map((column) => (
                <th
                  className="whitespace-nowrap border-b border-border px-4 py-3 text-left text-[11px] font-extrabold uppercase tracking-wide text-muted"
                  key={column.key}
                  style={{ width: column.width }}
                >
                  {column.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((row) => (
              <tr className="border-b border-border last:border-b-0 hover:bg-primary-faint/70" key={getRowId(row)}>
                {columns.map((column) => (
                  <td className={`px-4 py-3 align-middle ${column.className ?? ''}`} key={column.key}>
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {onPageChange ? (
        <div className="flex items-center justify-between border-t border-border bg-surface-low px-4 py-3 text-sm text-muted">
          <span>
            Trang <b className="text-text">{page}</b> / {pages} · {total} mục
          </span>
          <div className="flex items-center gap-2">
            <button className="icon-btn" disabled={page <= 1} onClick={() => onPageChange(page - 1)} type="button">
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button className="icon-btn" disabled={page >= pages} onClick={() => onPageChange(page + 1)} type="button">
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
