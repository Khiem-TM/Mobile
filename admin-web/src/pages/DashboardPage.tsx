import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Activity, Bot, Dumbbell, FileText, Server, Users } from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { ChartCard } from '../components/ChartCard';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { KpiCard } from '../components/KpiCard';
import { LoadingState } from '../components/LoadingState';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { get, patch } from '../lib/api';
import { formatNumber, formatShortDate, getItems } from '../lib/format';
import type { AdminStats, AnalyticsResponse, ApiErrorShape, Blog, HealthResponse, Paginated } from '../types';

export function DashboardPage() {
  const queryClient = useQueryClient();
  const statsQuery = useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => get<AdminStats>('/admin/stats'),
  });
  const overviewQuery = useQuery({
    queryKey: ['admin-analytics-overview'],
    queryFn: () => get<AnalyticsResponse>('/admin/analytics/overview'),
  });
  const healthQuery = useQuery({
    queryKey: ['admin-health'],
    queryFn: () => get<HealthResponse>('/admin/health'),
  });
  const pendingBlogsQuery = useQuery({
    queryKey: ['blogs', 'pending-dashboard'],
    queryFn: () => get<Paginated<Blog>>('/admin/blogs', { params: { page: 1, limit: 5, status: 'pending' } }),
  });

  const approveBlog = useMutation({
    mutationFn: (id: string) => patch<Blog>(`/admin/blogs/${id}/approve`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['blogs'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
    },
  });

  const stats = statsQuery.data ?? {};
  const overview = overviewQuery.data;
  const userSeries = overview?.users?.series ?? [];
  const nutritionSeries = overview?.nutrition?.series ?? [];
  const trainingSeries = overview?.training?.series ?? [];
  const blogSeries = overview?.blogs?.series ?? [];
  const aiSeries = overview?.ai?.scans ?? [];
  const mealTotal = nutritionSeries.reduce((sum, item) => sum + Number(item.meal_logs ?? 0), 0);

  if (statsQuery.isLoading && overviewQuery.isLoading) return <LoadingState label="Đang tải tổng quan hệ thống" />;
  if (statsQuery.error) {
    return <ErrorState message={(statsQuery.error as ApiErrorShape).message} onRetry={() => statsQuery.refetch()} />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <h1 className="page-title">Tổng quan hệ thống</h1>
          <p className="mt-1 text-sm text-muted">Dữ liệu thời gian thực và trạng thái hoạt động</p>
        </div>
        <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row">
          <div className="inline-flex rounded-lg border border-border bg-surface-high p-1">
            {['Ngày', 'Tuần', 'Tháng'].map((item) => (
              <button
                className={`h-9 rounded-md px-4 text-sm font-bold ${item === 'Tuần' ? 'bg-surface text-primary shadow-soft' : 'text-muted'}`}
                key={item}
                type="button"
              >
                {item}
              </button>
            ))}
          </div>
          <button className="btn-secondary" type="button">7 ngày qua</button>
        </div>
      </div>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          hint={`${formatNumber(stats.verifiedUsers)} đã xác minh`}
          icon={<Users className="h-5 w-5" />}
          label="Tổng người dùng"
          trend="+12%"
          value={formatNumber(stats.totalUsers)}
        />
        <KpiCard
          hint="Trong toàn hệ thống"
          icon={<Activity className="h-5 w-5" />}
          label="Đang hoạt động"
          trend="+5%"
          value={formatNumber(stats.activeUsers)}
        />
        <KpiCard
          hint="Global public food"
          icon={<Activity className="h-5 w-5" />}
          label="Thực phẩm hệ thống"
          value={formatNumber(stats.totalFoods)}
        />
        <KpiCard
          hint={`${formatNumber(stats.pendingBlogs)} blog chờ duyệt`}
          icon={<FileText className="h-5 w-5" />}
          label="Bài viết mới"
          tone="blue"
          value={formatNumber(stats.totalBlogs)}
        />
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,2fr)_minmax(360px,1fr)]">
        <ChartCard title="Tăng trưởng người dùng">
          <div className="h-72">
            {userSeries.length ? (
              <ResponsiveContainer height="100%" width="100%">
                <BarChart data={userSeries}>
                  <CartesianGrid stroke="#d3dac7" vertical={false} />
                  <XAxis dataKey="bucket" tickFormatter={formatShortDate} tickLine={false} />
                  <YAxis allowDecimals={false} tickLine={false} width={42} />
                  <Tooltip labelFormatter={(value) => formatShortDate(String(value))} />
                  <Bar dataKey="signups" fill="#4a7c59" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <EmptyState title="Chưa có dữ liệu tăng trưởng" />
            )}
          </div>
        </ChartCard>
        <ChartCard title="Nhật ký bữa ăn">
          <div className="relative grid h-72 place-items-center">
            <ResponsiveContainer height="100%" width="100%">
              <PieChart>
                <Pie
                  cx="50%"
                  cy="50%"
                  data={[
                    { name: 'Bữa ăn', value: mealTotal || stats.totalMealLogs || 0 },
                    { name: 'Còn lại', value: Math.max(Number(stats.totalMealLogs ?? 0) - mealTotal, 0) || 1 },
                  ]}
                  dataKey="value"
                  innerRadius={70}
                  outerRadius={104}
                  paddingAngle={3}
                >
                  <Cell fill="#4a7c59" />
                  <Cell fill="#dbe2d0" />
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
            <div className="pointer-events-none absolute text-center">
              <p className="text-3xl font-extrabold">{formatNumber(stats.totalMealLogs)}</p>
              <p className="text-sm font-bold text-muted">Bữa ăn</p>
            </div>
          </div>
        </ChartCard>
      </section>

      <section className="grid gap-4 xl:grid-cols-3">
        <ChartCard title="Hoạt động tập luyện">
          <MiniLineChart data={trainingSeries} dataKey="sessions" />
        </ChartCard>
        <ChartCard title="Blog & CMS">
          <MiniLineChart data={blogSeries} dataKey="posts" />
        </ChartCard>
        <ChartCard title="AI usage">
          <MiniLineChart data={aiSeries} dataKey="scans" />
        </ChartCard>
      </section>

      <section className="grid gap-4 xl:grid-cols-[minmax(320px,0.8fr)_minmax(0,1.8fr)]">
        <ChartCard
          action={<StatusBadge variant={statusVariant(healthQuery.data?.status)}>{healthQuery.data?.status ?? 'unknown'}</StatusBadge>}
          title="Trạng thái hệ thống"
        >
          <div className="space-y-2">
            {Object.entries(healthQuery.data?.services ?? {}).map(([name, service]) => (
              <div className="flex items-center justify-between rounded-lg border border-border bg-surface-low px-3 py-2.5" key={name}>
                <div className="flex min-w-0 items-center gap-2">
                  <Server className="h-4 w-4 shrink-0 text-muted" />
                  <span className="truncate text-sm font-bold capitalize text-text">{name}</span>
                </div>
                <StatusBadge variant={statusVariant(service.status)}>{service.status ?? '-'}</StatusBadge>
              </div>
            ))}
          </div>
        </ChartCard>
        <ChartCard
          action={
            <div className="flex gap-2">
              <span className="rounded-full bg-primary px-4 py-2 text-xs font-extrabold text-white">Blog ({stats.pendingBlogs ?? 0})</span>
            </div>
          }
          title="Cần kiểm duyệt"
        >
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-[11px] font-extrabold uppercase tracking-wide text-muted">
                  <th className="px-3 py-2">Tên mục</th>
                  <th className="px-3 py-2">Trạng thái</th>
                  <th className="px-3 py-2">Thời gian</th>
                  <th className="px-3 py-2 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {getItems(pendingBlogsQuery.data).map((blog) => (
                  <tr className="border-b border-border last:border-0" key={`blog-${blog.id}`}>
                    <td className="px-3 py-3 font-bold">{blog.title}</td>
                    <td className="px-3 py-3 text-muted">Pending</td>
                    <td className="px-3 py-3 font-mono text-xs">{formatShortDate(blog.createdAt ?? blog.created_at)}</td>
                    <td className="px-3 py-3 text-right">
                      <button className="text-sm font-bold text-primary" onClick={() => approveBlog.mutate(blog.id)} type="button">Duyệt</button>
                    </td>
                  </tr>
                ))}
                {!getItems(pendingBlogsQuery.data).length ? (
                  <tr>
                    <td className="px-3 py-6 text-center text-muted" colSpan={4}>Không có blog chờ duyệt</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </ChartCard>
      </section>
    </div>
  );
}

function MiniLineChart({ data, dataKey }: { data: Array<Record<string, string | number>>; dataKey: string }) {
  return (
    <div className="h-44">
      {data.length ? (
        <ResponsiveContainer height="100%" width="100%">
          <LineChart data={data}>
            <CartesianGrid stroke="#d3dac7" vertical={false} />
            <XAxis dataKey="bucket" tickFormatter={formatShortDate} tickLine={false} />
            <YAxis allowDecimals={false} tickLine={false} width={36} />
            <Tooltip labelFormatter={(value) => formatShortDate(String(value))} />
            <Line dataKey={dataKey} dot={false} stroke="#4a7c59" strokeWidth={3} type="monotone" />
          </LineChart>
        </ResponsiveContainer>
      ) : (
        <EmptyState title="Chưa có dữ liệu" />
      )}
    </div>
  );
}
