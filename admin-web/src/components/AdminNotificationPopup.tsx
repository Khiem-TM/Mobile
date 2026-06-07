import { useEffect, useMemo, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Bell, CheckCircle2, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';
import { get } from '../lib/api';
import { formatDate, getItems, getUserDisplayName } from '../lib/format';
import type { Blog, Paginated } from '../types';

const SEEN_PENDING_BLOG_IDS_KEY = 'admin-web:seen-pending-blog-ids';

export function AdminNotificationPopup({ enabled }: { enabled: boolean }) {
  const [open, setOpen] = useState(false);
  const initialized = useRef(false);

  const pendingBlogsQuery = useQuery({
    queryKey: ['blogs', 'pending-notifications'],
    queryFn: () => get<Paginated<Blog>>('/admin/blogs', { params: { status: 'pending', page: 1, limit: 5 } }),
    enabled,
    refetchInterval: enabled ? 30_000 : false,
  });

  const pendingBlogs = useMemo(() => getItems(pendingBlogsQuery.data), [pendingBlogsQuery.data]);
  const pendingTotal = pendingBlogsQuery.data?.total ?? 0;

  useEffect(() => {
    if (!enabled || !pendingBlogsQuery.data) return;
    const ids = pendingBlogs.map((blog) => blog.id);
    const seenIds = readSeenIds();
    const unseenIds = ids.filter((id) => !seenIds.includes(id));

    if (!initialized.current) {
      initialized.current = true;
      writeSeenIds([...seenIds, ...ids]);
      return;
    }

    if (unseenIds.length > 0) {
      setOpen(true);
      writeSeenIds([...seenIds, ...unseenIds]);
    }
  }, [enabled, pendingBlogs, pendingBlogsQuery.data]);

  return (
    <div className="relative">
      <button className="icon-btn relative" onClick={() => setOpen((value) => !value)} type="button">
        <Bell className="h-5 w-5" />
        {pendingTotal > 0 ? (
          <span className="absolute -right-1 -top-1 min-w-5 rounded-full bg-danger px-1.5 py-0.5 text-[10px] font-extrabold leading-none text-white">
            {pendingTotal > 99 ? '99+' : pendingTotal}
          </span>
        ) : null}
      </button>

      {open ? (
        <div className="absolute right-0 top-12 z-50 w-[min(360px,calc(100vw-24px))] overflow-hidden rounded-lg border border-border bg-surface shadow-float">
          <div className="flex items-center justify-between border-b border-border bg-surface-low px-4 py-3">
            <div>
              <p className="text-sm font-extrabold text-text">Blog chờ duyệt</p>
              <p className="text-xs font-semibold text-muted">{pendingTotal} bài đang trong hàng chờ</p>
            </div>
            <FileText className="h-5 w-5 text-primary" />
          </div>

          <div className="max-h-96 overflow-y-auto">
            {pendingBlogs.map((blog) => (
              <Link
                className="block border-b border-border px-4 py-3 transition last:border-b-0 hover:bg-primary-faint"
                key={blog.id}
                onClick={() => setOpen(false)}
                to={`/blogs/${blog.id}`}
              >
                <p className="line-clamp-2 text-sm font-extrabold text-text">{blog.title}</p>
                <p className="mt-1 truncate text-xs text-muted">
                  {blog.authorUser ? getUserDisplayName(blog.authorUser) : blog.author ?? 'Người dùng'} · {formatDate(blog.createdAt ?? blog.created_at)}
                </p>
              </Link>
            ))}
            {!pendingBlogs.length ? (
              <div className="grid gap-2 px-4 py-8 text-center">
                <CheckCircle2 className="mx-auto h-8 w-8 text-primary" />
                <p className="text-sm font-bold text-text">Không có blog chờ duyệt</p>
                <p className="text-xs text-muted">Hàng chờ moderation đang trống.</p>
              </div>
            ) : null}
          </div>

          <div className="grid grid-cols-2 gap-2 border-t border-border bg-surface-low p-3">
            <button className="btn-secondary h-9" onClick={() => setOpen(false)} type="button">
              Đóng
            </button>
            <Link className="btn-primary h-9" onClick={() => setOpen(false)} to="/blogs?status=pending">
              Xem hàng chờ
            </Link>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function readSeenIds() {
  try {
    const raw = localStorage.getItem(SEEN_PENDING_BLOG_IDS_KEY);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string') : [];
  } catch {
    return [];
  }
}

function writeSeenIds(ids: string[]) {
  const uniqueIds = Array.from(new Set(ids)).slice(-100);
  localStorage.setItem(SEEN_PENDING_BLOG_IDS_KEY, JSON.stringify(uniqueIds));
}
