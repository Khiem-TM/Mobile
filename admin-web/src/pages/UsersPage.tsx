import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Ban, CheckCircle2, Edit, Eye, Plus, RotateCcw, Send, ShieldCheck } from 'lucide-react';
import { z } from 'zod';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataTable, type DataColumn } from '../components/DataTable';
import { DrawerForm } from '../components/DrawerForm';
import { FilterBar } from '../components/FilterBar';
import { FormField } from '../components/FormField';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { compactParams, get, patch, post } from '../lib/api';
import {
  boolFromSelect,
  formatDate,
  getItems,
  getUserDisplayName,
  isUserActive,
  isUserVerified,
} from '../lib/format';
import type { AdminUser, ApiErrorShape, Paginated } from '../types';

const createUserSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(8, 'Mật khẩu tối thiểu 8 ký tự'),
  displayName: z.string().min(2, 'Tên hiển thị tối thiểu 2 ký tự'),
  avatarUrl: z.string().url('URL ảnh không hợp lệ').or(z.literal('')).optional(),
  role: z.enum(['user', 'admin']).optional(),
  isVerified: z.boolean().optional(),
  isActive: z.boolean().optional(),
});

const updateUserSchema = createUserSchema.partial().extend({
  password: z.string().min(8, 'Mật khẩu tối thiểu 8 ký tự').or(z.literal('')).optional(),
});

const warningSchema = z.object({
  title: z.string().min(1, 'Vui lòng nhập tiêu đề').max(200),
  body: z.string().min(1, 'Vui lòng nhập nội dung'),
  reasonCode: z.string().max(100).or(z.literal('')).optional(),
});

type UserFilters = {
  search: string;
  role: string;
  isActive: string;
  isVerified: string;
  createdFrom: string;
  createdTo: string;
};

const initialFilters: UserFilters = {
  search: '',
  role: '',
  isActive: '',
  isVerified: '',
  createdFrom: '',
  createdTo: '',
};

export function UsersPage() {
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState(initialFilters);
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState<{ mode: 'create' | 'edit'; user?: AdminUser } | null>(null);
  const [warningUser, setWarningUser] = useState<AdminUser | null>(null);
  const [confirmUser, setConfirmUser] = useState<{ type: 'ban' | 'unban'; user: AdminUser } | null>(null);
  const [formError, setFormError] = useState('');

  const params = useMemo(() => compactParams({ page, limit: 20, ...filters }), [filters, page]);
  const usersQuery = useQuery({
    queryKey: ['users', params],
    queryFn: () => get<Paginated<AdminUser>>('/admin/users', { params }),
  });

  const createUser = useMutation({
    mutationFn: (payload: Record<string, unknown>) => post<AdminUser>('/admin/users', payload),
    onSuccess: () => closeAndRefresh(),
  });
  const updateUser = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) => patch<AdminUser>(`/admin/users/${id}`, payload),
    onSuccess: () => closeAndRefresh(),
  });
  const banUser = useMutation({
    mutationFn: ({ id, type }: { id: string; type: 'ban' | 'unban' }) => patch<AdminUser>(`/admin/users/${id}/${type}`),
    onSuccess: () => {
      setConfirmUser(null);
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
    },
  });
  const verifyEmail = useMutation({
    mutationFn: (id: string) => patch<AdminUser>(`/admin/users/${id}/verify-email`),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });
  const sendWarning = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) => post(`/admin/users/${id}/warnings`, payload),
    onSuccess: () => {
      setWarningUser(null);
      setFormError('');
    },
  });

  function closeAndRefresh() {
    setDrawer(null);
    setFormError('');
    queryClient.invalidateQueries({ queryKey: ['users'] });
    queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
  }

  function submitUserForm(formData: FormData) {
    const raw = {
      email: String(formData.get('email') ?? '').trim(),
      password: String(formData.get('password') ?? ''),
      displayName: String(formData.get('displayName') ?? '').trim(),
      avatarUrl: String(formData.get('avatarUrl') ?? '').trim(),
      role: String(formData.get('role') ?? 'user') as 'user' | 'admin',
      isVerified: boolFromSelect(String(formData.get('isVerified') ?? 'true')),
      isActive: boolFromSelect(String(formData.get('isActive') ?? 'true')),
    };
    const schema = drawer?.mode === 'edit' ? updateUserSchema : createUserSchema;
    const parsed = schema.safeParse(raw);
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu không hợp lệ');
      return;
    }
    const payload = Object.fromEntries(
      Object.entries(parsed.data).filter(([, value]) => value !== '' && value !== undefined),
    );
    if (drawer?.mode === 'edit' && drawer.user) updateUser.mutate({ id: drawer.user.id, payload });
    else createUser.mutate(payload);
  }

  function submitWarning(formData: FormData) {
    if (!warningUser) return;
    const parsed = warningSchema.safeParse({
      title: String(formData.get('title') ?? ''),
      body: String(formData.get('body') ?? ''),
      reasonCode: String(formData.get('reasonCode') ?? ''),
    });
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu cảnh báo không hợp lệ');
      return;
    }
    const payload = Object.fromEntries(Object.entries(parsed.data).filter(([, value]) => value !== ''));
    sendWarning.mutate({ id: warningUser.id, payload });
  }

  const columns: DataColumn<AdminUser>[] = [
    {
      key: 'name',
      header: 'Người dùng',
      render: (user) => (
        <div className="min-w-0">
          <Link className="font-extrabold text-text hover:text-primary" to={`/users/${user.id}`}>
            {getUserDisplayName(user)}
          </Link>
          <p className="truncate text-xs text-muted">{user.email}</p>
        </div>
      ),
    },
    { key: 'role', header: 'Vai trò', render: (user) => <StatusBadge variant="info">{user.role}</StatusBadge> },
    {
      key: 'active',
      header: 'Trạng thái',
      render: (user) => (
        <StatusBadge variant={statusVariant(isUserActive(user))}>{isUserActive(user) ? 'Hoạt động' : 'Đã khóa'}</StatusBadge>
      ),
    },
    {
      key: 'verified',
      header: 'Email',
      render: (user) => (
        <StatusBadge variant={statusVariant(isUserVerified(user))}>{isUserVerified(user) ? 'Đã xác minh' : 'Chưa xác minh'}</StatusBadge>
      ),
    },
    { key: 'created', header: 'Ngày tạo', render: (user) => <span className="font-mono text-xs">{formatDate(user.created_at ?? user.createdAt)}</span> },
    {
      key: 'actions',
      header: 'Thao tác',
      className: 'text-right',
      render: (user) => (
        <div className="flex justify-end gap-1">
          <Link className="icon-btn" to={`/users/${user.id}`}>
            <Eye className="h-4 w-4" />
          </Link>
          <button className="icon-btn" onClick={() => setDrawer({ mode: 'edit', user })} type="button">
            <Edit className="h-4 w-4" />
          </button>
          <button className="icon-btn" disabled={isUserVerified(user)} onClick={() => verifyEmail.mutate(user.id)} type="button">
            <ShieldCheck className="h-4 w-4" />
          </button>
          <button className="icon-btn" onClick={() => setWarningUser(user)} type="button">
            <Send className="h-4 w-4" />
          </button>
          <button className="icon-btn text-danger hover:text-danger" onClick={() => setConfirmUser({ type: isUserActive(user) ? 'ban' : 'unban', user })} type="button">
            {isUserActive(user) ? <Ban className="h-4 w-4" /> : <CheckCircle2 className="h-4 w-4" />}
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="page-title">Người dùng</h1>
          <p className="mt-1 text-sm text-muted">Quản trị tài khoản, vai trò, xác minh và cảnh báo</p>
        </div>
        <button className="btn-primary" onClick={() => setDrawer({ mode: 'create' })} type="button">
          <Plus className="h-4 w-4" />
          Thêm người dùng
        </button>
      </div>

      <FilterBar
        actions={
          <button
            className="btn-secondary"
            onClick={() => {
              setFilters(initialFilters);
              setPage(1);
            }}
            type="button"
          >
            <RotateCcw className="h-4 w-4" />
            Đặt lại
          </button>
        }
      >
        <input className="input" placeholder="Tìm email, tên..." value={filters.search} onChange={(event) => setFilters({ ...filters, search: event.target.value })} />
        <select className="select" value={filters.role} onChange={(event) => setFilters({ ...filters, role: event.target.value })}>
          <option value="">Tất cả vai trò</option>
          <option value="user">User</option>
          <option value="admin">Admin</option>
        </select>
        <select className="select" value={filters.isActive} onChange={(event) => setFilters({ ...filters, isActive: event.target.value })}>
          <option value="">Tất cả trạng thái</option>
          <option value="true">Hoạt động</option>
          <option value="false">Đã khóa</option>
        </select>
        <select className="select" value={filters.isVerified} onChange={(event) => setFilters({ ...filters, isVerified: event.target.value })}>
          <option value="">Tất cả xác minh</option>
          <option value="true">Đã xác minh</option>
          <option value="false">Chưa xác minh</option>
        </select>
        <input className="input" type="date" value={filters.createdFrom} onChange={(event) => setFilters({ ...filters, createdFrom: event.target.value })} />
      </FilterBar>

      <DataTable
        columns={columns}
        data={getItems(usersQuery.data)}
        emptyTitle="Chưa có người dùng phù hợp"
        error={(usersQuery.error as ApiErrorShape | null)?.message}
        getRowId={(user) => user.id}
        isLoading={usersQuery.isLoading}
        limit={usersQuery.data?.limit}
        onPageChange={setPage}
        page={usersQuery.data?.page ?? page}
        total={usersQuery.data?.total}
      />

      <DrawerForm
        footer={
          <div className="flex justify-end gap-2">
            <button className="btn-secondary" onClick={() => setDrawer(null)} type="button">Hủy</button>
            <button className="btn-primary" disabled={createUser.isPending || updateUser.isPending} form="user-form" type="submit">
              {drawer?.mode === 'edit' ? 'Lưu thay đổi' : 'Tạo người dùng'}
            </button>
          </div>
        }
        onClose={() => setDrawer(null)}
        open={Boolean(drawer)}
        title={drawer?.mode === 'edit' ? 'Cập nhật người dùng' : 'Thêm người dùng'}
      >
        <form
          className="grid gap-4"
          id="user-form"
          onSubmit={(event) => {
            event.preventDefault();
            submitUserForm(new FormData(event.currentTarget));
          }}
        >
          <UserFormFields user={drawer?.user} mode={drawer?.mode ?? 'create'} />
          {formError || createUser.error || updateUser.error ? (
            <div className="rounded-md border border-danger/25 bg-danger-soft/40 px-3 py-2 text-sm font-semibold text-danger">
              {formError || (createUser.error as ApiErrorShape | null)?.message || (updateUser.error as ApiErrorShape | null)?.message}
            </div>
          ) : null}
        </form>
      </DrawerForm>

      <DrawerForm
        footer={
          <div className="flex justify-end gap-2">
            <button className="btn-secondary" onClick={() => setWarningUser(null)} type="button">Hủy</button>
            <button className="btn-primary" disabled={sendWarning.isPending} form="warning-form" type="submit">Gửi cảnh báo</button>
          </div>
        }
        onClose={() => setWarningUser(null)}
        open={Boolean(warningUser)}
        title="Gửi cảnh báo"
        description={warningUser ? getUserDisplayName(warningUser) : undefined}
      >
        <form
          className="grid gap-4"
          id="warning-form"
          onSubmit={(event) => {
            event.preventDefault();
            submitWarning(new FormData(event.currentTarget));
          }}
        >
          <FormField label="Tiêu đề"><input className="input" name="title" /></FormField>
          <FormField label="Nội dung"><textarea className="textarea" name="body" /></FormField>
          <FormField label="Mã lý do"><input className="input" name="reasonCode" placeholder="blog:spam" /></FormField>
          {formError || sendWarning.error ? (
            <div className="rounded-md border border-danger/25 bg-danger-soft/40 px-3 py-2 text-sm font-semibold text-danger">
              {formError || (sendWarning.error as ApiErrorShape | null)?.message}
            </div>
          ) : null}
        </form>
      </DrawerForm>

      <ConfirmDialog
        confirmLabel={confirmUser?.type === 'ban' ? 'Khóa tài khoản' : 'Mở khóa'}
        description={confirmUser ? `${getUserDisplayName(confirmUser.user)} sẽ được cập nhật trạng thái.` : undefined}
        isLoading={banUser.isPending}
        onCancel={() => setConfirmUser(null)}
        onConfirm={() => confirmUser && banUser.mutate({ id: confirmUser.user.id, type: confirmUser.type })}
        open={Boolean(confirmUser)}
        title={confirmUser?.type === 'ban' ? 'Khóa tài khoản?' : 'Mở khóa tài khoản?'}
        tone={confirmUser?.type === 'ban' ? 'danger' : 'primary'}
      />
    </div>
  );
}

function UserFormFields({ user, mode }: { user?: AdminUser; mode: 'create' | 'edit' }) {
  return (
    <>
      <FormField label="Email"><input className="input" defaultValue={user?.email} name="email" type="email" /></FormField>
      <FormField label="Tên hiển thị"><input className="input" defaultValue={getUserDisplayName(user)} name="displayName" /></FormField>
      <FormField label={mode === 'create' ? 'Mật khẩu' : 'Mật khẩu mới'}>
        <input className="input" name="password" type="password" />
      </FormField>
      <FormField label="Avatar URL"><input className="input" defaultValue={user?.avatar_url ?? user?.avatarUrl ?? ''} name="avatarUrl" /></FormField>
      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Vai trò">
          <select className="select" defaultValue={user?.role ?? 'user'} name="role">
            <option value="user">User</option>
            <option value="admin">Admin</option>
          </select>
        </FormField>
        <FormField label="Xác minh email">
          <select className="select" defaultValue={String(user ? isUserVerified(user) : true)} name="isVerified">
            <option value="true">Đã xác minh</option>
            <option value="false">Chưa xác minh</option>
          </select>
        </FormField>
        <FormField label="Trạng thái">
          <select className="select" defaultValue={String(user ? isUserActive(user) : true)} name="isActive">
            <option value="true">Hoạt động</option>
            <option value="false">Đã khóa</option>
          </select>
        </FormField>
      </div>
    </>
  );
}
