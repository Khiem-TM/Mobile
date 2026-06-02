import { useState } from 'react';
import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Bot, Dumbbell, FileText, Mail, Salad } from 'lucide-react';
import { ChartCard } from '../components/ChartCard';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { get } from '../lib/api';
import { formatDate, formatNumber, getUserDisplayName, isUserActive, isUserVerified } from '../lib/format';
import type { AdminUser, ApiErrorShape } from '../types';

const tabs = ['profile', 'health', 'workouts', 'activity'] as const;
type Tab = (typeof tabs)[number];

export function UserDetailPage() {
  const { id } = useParams();
  const [tab, setTab] = useState<Tab>('profile');
  const userQuery = useQuery({
    queryKey: ['users', id],
    queryFn: () => get<AdminUser>(`/admin/users/${id}`),
    enabled: Boolean(id),
  });

  if (userQuery.isLoading) return <LoadingState label="Đang tải hồ sơ người dùng" />;
  if (userQuery.error || !userQuery.data) {
    return <ErrorState message={(userQuery.error as ApiErrorShape | null)?.message ?? 'Không tìm thấy người dùng'} />;
  }

  const user = userQuery.data;
  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <Link className="btn-secondary" to="/users">
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </Link>
      </div>

      <section className="card p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex min-w-0 items-center gap-4">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-primary text-2xl font-extrabold text-white">
              {getUserDisplayName(user).slice(0, 1).toUpperCase()}
            </div>
            <div className="min-w-0">
              <h1 className="truncate text-2xl font-extrabold text-text">{getUserDisplayName(user)}</h1>
              <p className="mt-1 flex items-center gap-2 truncate text-sm text-muted">
                <Mail className="h-4 w-4 shrink-0" />
                {user.email}
              </p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <StatusBadge variant={statusVariant(isUserActive(user))}>{isUserActive(user) ? 'Hoạt động' : 'Đã khóa'}</StatusBadge>
            <StatusBadge variant={statusVariant(isUserVerified(user))}>{isUserVerified(user) ? 'Đã xác minh' : 'Chưa xác minh'}</StatusBadge>
            <StatusBadge variant="info">{user.role}</StatusBadge>
          </div>
        </div>
      </section>

      <div className="flex overflow-x-auto rounded-lg border border-border bg-surface p-1">
        {[
          ['profile', 'Hồ sơ'],
          ['health', 'Health profile'],
          ['workouts', 'Tập luyện gần đây'],
          ['activity', 'Tổng hợp hoạt động'],
        ].map(([key, label]) => (
          <button
            className={`h-9 whitespace-nowrap rounded-md px-4 text-sm font-bold ${tab === key ? 'bg-primary text-white' : 'text-muted hover:bg-primary-faint'}`}
            key={key}
            onClick={() => setTab(key as Tab)}
            type="button"
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'profile' ? <ProfileTab user={user} /> : null}
      {tab === 'health' ? <JsonTab data={user.healthProfile} empty="Chưa có health profile" /> : null}
      {tab === 'workouts' ? <WorkoutsTab user={user} /> : null}
      {tab === 'activity' ? <ActivityTab user={user} /> : null}
    </div>
  );
}

function ProfileTab({ user }: { user: AdminUser }) {
  return (
    <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <InfoCard label="ID" value={user.id} mono />
      <InfoCard label="Email" value={user.email} />
      <InfoCard label="Ngày tạo" value={formatDate(user.created_at ?? user.createdAt)} />
      <InfoCard label="Cập nhật" value={formatDate(user.updated_at ?? user.updatedAt)} />
    </section>
  );
}

function WorkoutsTab({ user }: { user: AdminUser }) {
  const sessions = user.recentSessions ?? [];
  return (
    <ChartCard title="Buổi tập gần đây">
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left text-[11px] font-extrabold uppercase tracking-wide text-muted">
              <th className="px-3 py-2">Thời gian</th>
              <th className="px-3 py-2">Thời lượng</th>
              <th className="px-3 py-2">Calories</th>
              <th className="px-3 py-2">Items</th>
            </tr>
          </thead>
          <tbody>
            {sessions.map((session) => (
              <tr className="border-b border-border last:border-0" key={session.id}>
                <td className="px-3 py-3 font-mono text-xs">{formatDate(session.sessionDate ?? session.createdAt)}</td>
                <td className="px-3 py-3">{formatNumber(session.totalDurationMinutes, ' phút')}</td>
                <td className="px-3 py-3">{formatNumber(session.totalCaloriesBurned, ' kcal')}</td>
                <td className="px-3 py-3">{session.items?.length ?? 0}</td>
              </tr>
            ))}
            {!sessions.length ? (
              <tr>
                <td className="px-3 py-6 text-center text-muted" colSpan={4}>Chưa có lịch sử tập luyện</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </ChartCard>
  );
}

function ActivityTab({ user }: { user: AdminUser }) {
  const summary = user.adminSummary ?? {};
  return (
    <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <InfoCard icon={<Salad className="h-5 w-5" />} label="Meal logs" value={formatNumber(summary.mealLogCount)} />
      <InfoCard icon={<Dumbbell className="h-5 w-5" />} label="Training sessions" value={formatNumber(user.recentSessions?.length)} />
      <InfoCard icon={<FileText className="h-5 w-5" />} label="Bài viết" value={formatNumber(summary.blogCount)} />
      <InfoCard icon={<Bot className="h-5 w-5" />} label="AI scans" value={formatNumber(summary.aiScanCount)} />
    </section>
  );
}

function JsonTab({ data, empty }: { data: unknown; empty: string }) {
  if (!data) return <ChartCard title={empty}><div className="text-sm text-muted">{empty}</div></ChartCard>;
  return (
    <ChartCard title="Dữ liệu chi tiết">
      <pre className="max-h-[520px] overflow-auto rounded-lg bg-surface-low p-4 text-xs text-text">
        {JSON.stringify(data, null, 2)}
      </pre>
    </ChartCard>
  );
}

function InfoCard({ label, value, mono, icon }: { label: string; value?: string | number; mono?: boolean; icon?: ReactNode }) {
  return (
    <div className="card p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <p className="label">{label}</p>
        {icon ? <span className="text-primary">{icon}</span> : null}
      </div>
      <p className={`break-words text-xl font-extrabold text-text ${mono ? 'font-mono text-sm' : ''}`}>{value ?? '-'}</p>
    </div>
  );
}
