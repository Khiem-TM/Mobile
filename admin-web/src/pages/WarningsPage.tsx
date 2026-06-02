import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { AlertTriangle, Send } from 'lucide-react';
import { z } from 'zod';
import { ChartCard } from '../components/ChartCard';
import { EmptyState } from '../components/EmptyState';
import { FormField } from '../components/FormField';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { get, post } from '../lib/api';
import { getItems, getUserDisplayName, isUserActive } from '../lib/format';
import type { AdminUser, ApiErrorShape, Paginated } from '../types';

const warningSchema = z.object({
  userId: z.string().min(1, 'Vui lòng chọn người dùng'),
  title: z.string().min(1, 'Vui lòng nhập tiêu đề').max(200),
  body: z.string().min(1, 'Vui lòng nhập nội dung'),
  reasonCode: z.string().max(100).or(z.literal('')).optional(),
});

export function WarningsPage() {
  const [search, setSearch] = useState('');
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [formError, setFormError] = useState('');
  const usersQuery = useQuery({
    queryKey: ['users', 'warning-search', search],
    queryFn: () => get<Paginated<AdminUser>>('/admin/users', { params: { page: 1, limit: 8, search } }),
    enabled: search.trim().length >= 2,
  });
  const sendWarning = useMutation({
    mutationFn: ({ userId, payload }: { userId: string; payload: Record<string, unknown> }) => post(`/admin/users/${userId}/warnings`, payload),
    onSuccess: () => {
      setFormError('');
      setSelectedUser(null);
      setSearch('');
    },
  });

  function submitWarning(formData: FormData) {
    const parsed = warningSchema.safeParse({
      userId: selectedUser?.id ?? '',
      title: String(formData.get('title') ?? ''),
      body: String(formData.get('body') ?? ''),
      reasonCode: String(formData.get('reasonCode') ?? ''),
    });
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu cảnh báo không hợp lệ');
      return;
    }
    const { userId, ...rest } = parsed.data;
    const payload = Object.fromEntries(Object.entries(rest).filter(([, value]) => value !== ''));
    sendWarning.mutate({ userId, payload });
  }

  return (
    <div className="space-y-5">
      <div>
        <h1 className="page-title">Cảnh báo</h1>
        <p className="mt-1 text-sm text-muted">Gửi notification warning cho user từ thao tác moderation</p>
      </div>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(360px,0.8fr)]">
        <ChartCard title="Gửi cảnh báo">
          <form
            className="grid gap-4"
            onSubmit={(event) => {
              event.preventDefault();
              submitWarning(new FormData(event.currentTarget));
            }}
          >
            <FormField label="Tìm người dùng">
              <input className="input" placeholder="Nhập email hoặc tên" value={search} onChange={(event) => setSearch(event.target.value)} />
            </FormField>
            {selectedUser ? (
              <div className="rounded-lg border border-primary/20 bg-primary-faint p-3">
                <p className="font-extrabold text-text">{getUserDisplayName(selectedUser)}</p>
                <p className="text-sm text-muted">{selectedUser.email}</p>
              </div>
            ) : null}
            <FormField label="Tiêu đề"><input className="input" name="title" placeholder="Cảnh báo vi phạm nội dung" /></FormField>
            <FormField label="Nội dung"><textarea className="textarea" name="body" placeholder="Vui lòng chỉnh sửa nội dung theo quy định cộng đồng." /></FormField>
            <FormField label="Reason code"><input className="input" name="reasonCode" placeholder="blog:spam" /></FormField>
            {formError || sendWarning.error ? (
              <div className="rounded-md border border-danger/25 bg-danger-soft/40 px-3 py-2 text-sm font-semibold text-danger">
                {formError || (sendWarning.error as ApiErrorShape | null)?.message}
              </div>
            ) : null}
            {sendWarning.isSuccess ? (
              <div className="rounded-md border border-primary/25 bg-primary-soft px-3 py-2 text-sm font-semibold text-primary-dark">
                Đã gửi cảnh báo
              </div>
            ) : null}
            <button className="btn-primary w-fit" disabled={sendWarning.isPending} type="submit">
              <Send className="h-4 w-4" />
              Gửi cảnh báo
            </button>
          </form>
        </ChartCard>

        <ChartCard title="Kết quả tìm kiếm">
          <div className="space-y-2">
            {getItems(usersQuery.data).map((user) => (
              <button
                className={`w-full rounded-lg border p-3 text-left transition ${
                  selectedUser?.id === user.id ? 'border-primary bg-primary-faint' : 'border-border bg-surface-low hover:border-primary/50'
                }`}
                key={user.id}
                onClick={() => setSelectedUser(user)}
                type="button"
              >
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="truncate font-extrabold text-text">{getUserDisplayName(user)}</p>
                    <p className="truncate text-sm text-muted">{user.email}</p>
                  </div>
                  <StatusBadge variant={statusVariant(isUserActive(user))}>{isUserActive(user) ? 'Active' : 'Inactive'}</StatusBadge>
                </div>
              </button>
            ))}
            {!usersQuery.data && search.length < 2 ? (
              <div className="rounded-lg border border-border bg-surface-low p-5 text-center text-sm text-muted">
                <AlertTriangle className="mx-auto mb-2 h-5 w-5" />
                Chưa có kết quả tìm kiếm
              </div>
            ) : null}
            {usersQuery.data && !getItems(usersQuery.data).length ? <EmptyState title="Không tìm thấy user" /> : null}
          </div>
        </ChartCard>
      </section>
    </div>
  );
}
