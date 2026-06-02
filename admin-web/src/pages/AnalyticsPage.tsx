import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Activity, Bot, Dumbbell, FileText, Salad, Users } from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
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
import { compactParams, get } from '../lib/api';
import { daysAgoIso, formatNumber, formatShortDate, todayIso } from '../lib/format';
import type { AnalyticsResponse, ApiErrorShape, Granularity } from '../types';

const tabs = [
  { key: 'users', label: 'Users', icon: Users },
  { key: 'nutrition', label: 'Nutrition', icon: Salad },
  { key: 'training', label: 'Training', icon: Dumbbell },
  { key: 'blogs', label: 'Blogs', icon: FileText },
  { key: 'ai', label: 'AI', icon: Bot },
] as const;

type TabKey = (typeof tabs)[number]['key'];

export function AnalyticsPage() {
  const [tab, setTab] = useState<TabKey>('users');
  const [fromDate, setFromDate] = useState(daysAgoIso(29));
  const [toDate, setToDate] = useState(todayIso());
  const [granularity, setGranularity] = useState<Granularity>('day');
  const params = useMemo(() => compactParams({ fromDate, toDate, granularity }), [fromDate, granularity, toDate]);
  const analyticsQuery = useQuery({
    queryKey: ['admin-analytics', tab, params],
    queryFn: () => get<AnalyticsResponse>(`/admin/analytics/${tab}`, { params }),
  });

  const data = analyticsQuery.data;

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <h1 className="page-title">Phân tích</h1>
          <p className="mt-1 text-sm text-muted">Theo dõi xu hướng vận hành theo ngày, tuần hoặc tháng</p>
        </div>
        <div className="grid grid-cols-1 gap-2 sm:grid-cols-3 xl:w-[620px]">
          <input className="input" type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} />
          <input className="input" type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} />
          <select className="select" value={granularity} onChange={(event) => setGranularity(event.target.value as Granularity)}>
            <option value="day">Ngày</option>
            <option value="week">Tuần</option>
            <option value="month">Tháng</option>
          </select>
        </div>
      </div>

      <div className="flex overflow-x-auto rounded-lg border border-border bg-surface p-1">
        {tabs.map((item) => {
          const Icon = item.icon;
          return (
            <button
              className={`flex h-10 items-center gap-2 whitespace-nowrap rounded-md px-4 text-sm font-extrabold ${
                tab === item.key ? 'bg-primary text-white' : 'text-muted hover:bg-primary-faint'
              }`}
              key={item.key}
              onClick={() => setTab(item.key)}
              type="button"
            >
              <Icon className="h-4 w-4" />
              {item.label}
            </button>
          );
        })}
      </div>

      {analyticsQuery.isLoading ? <LoadingState label="Đang tải analytics" /> : null}
      {analyticsQuery.error ? <ErrorState message={(analyticsQuery.error as ApiErrorShape).message} onRetry={() => analyticsQuery.refetch()} /> : null}
      {!analyticsQuery.isLoading && !analyticsQuery.error && data ? <AnalyticsContent data={data} tab={tab} /> : null}
    </div>
  );
}

function AnalyticsContent({ tab, data }: { tab: TabKey; data: AnalyticsResponse }) {
  if (tab === 'users') return <UsersAnalytics data={data} />;
  if (tab === 'nutrition') return <NutritionAnalytics data={data} />;
  if (tab === 'training') return <TrainingAnalytics data={data} />;
  if (tab === 'blogs') return <BlogsAnalytics data={data} />;
  return <AiAnalytics data={data} />;
}

function UsersAnalytics({ data }: { data: AnalyticsResponse }) {
  const series = data.series ?? [];
  return (
    <div className="space-y-4">
      <section className="grid gap-4 md:grid-cols-3">
        <KpiCard icon={<Users className="h-5 w-5" />} label="Tổng user" value={formatNumber(data.totals?.totalUsers)} />
        <KpiCard icon={<Activity className="h-5 w-5" />} label="Active user" value={formatNumber(data.totals?.activeUsers)} />
        <KpiCard icon={<Users className="h-5 w-5" />} label="Admin user" tone="blue" value={formatNumber(data.totals?.adminUsers)} />
      </section>
      <ChartCard title="User signups">
        <BarPanel data={series} keys={['signups']} />
      </ChartCard>
    </div>
  );
}

function NutritionAnalytics({ data }: { data: AnalyticsResponse }) {
  return (
    <div className="grid gap-4 xl:grid-cols-2">
      <ChartCard title="Meal logs"><BarPanel data={data.series ?? []} keys={['meal_logs', 'active_users']} /></ChartCard>
      <ChartCard title="Calories snapshot"><LinePanel data={data.series ?? []} keys={['total_calories']} /></ChartCard>
    </div>
  );
}

function TrainingAnalytics({ data }: { data: AnalyticsResponse }) {
  return (
    <div className="grid gap-4 xl:grid-cols-2">
      <ChartCard title="Training sessions"><BarPanel data={data.series ?? []} keys={['sessions', 'active_users']} /></ChartCard>
      <ChartCard title="Calories burned"><LinePanel data={data.series ?? []} keys={['total_calories_burned', 'total_duration_minutes']} /></ChartCard>
    </div>
  );
}

function BlogsAnalytics({ data }: { data: AnalyticsResponse }) {
  return (
    <div className="grid gap-4 xl:grid-cols-2">
      <ChartCard title="Moderation"><BarPanel data={data.series ?? []} keys={['posts', 'pending', 'approved', 'rejected']} /></ChartCard>
      <ChartCard title="Engagement"><LinePanel data={data.series ?? []} keys={['views', 'likes', 'comments']} /></ChartCard>
    </div>
  );
}

function AiAnalytics({ data }: { data: AnalyticsResponse }) {
  return (
    <div className="grid gap-4 xl:grid-cols-3">
      <ChartCard title="AI scans"><BarPanel data={data.scans ?? []} keys={['scans', 'users']} /></ChartCard>
      <ChartCard title="Chat sessions"><BarPanel data={data.chatSessions ?? []} keys={['sessions', 'users']} /></ChartCard>
      <ChartCard title="Messages"><LinePanel data={data.messages ?? []} keys={['messages', 'user_messages', 'assistant_messages']} /></ChartCard>
    </div>
  );
}

function BarPanel({ data, keys }: { data: Array<Record<string, string | number>>; keys: string[] }) {
  if (!data.length) return <EmptyState title="Chưa có dữ liệu" />;
  return (
    <div className="h-80">
      <ResponsiveContainer height="100%" width="100%">
        <BarChart data={data}>
          <CartesianGrid stroke="#d3dac7" vertical={false} />
          <XAxis dataKey="bucket" tickFormatter={formatShortDate} tickLine={false} />
          <YAxis allowDecimals={false} tickLine={false} width={44} />
          <Tooltip labelFormatter={(value) => formatShortDate(String(value))} />
          <Legend />
          {keys.map((key, index) => (
            <Bar dataKey={key} fill={['#4a7c59', '#39656d', '#a25d00', '#ba1a1a'][index % 4]} key={key} radius={[4, 4, 0, 0]} />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

function LinePanel({ data, keys }: { data: Array<Record<string, string | number>>; keys: string[] }) {
  if (!data.length) return <EmptyState title="Chưa có dữ liệu" />;
  return (
    <div className="h-80">
      <ResponsiveContainer height="100%" width="100%">
        <LineChart data={data}>
          <CartesianGrid stroke="#d3dac7" vertical={false} />
          <XAxis dataKey="bucket" tickFormatter={formatShortDate} tickLine={false} />
          <YAxis tickLine={false} width={50} />
          <Tooltip labelFormatter={(value) => formatShortDate(String(value))} />
          <Legend />
          {keys.map((key, index) => (
            <Line dataKey={key} dot={false} key={key} stroke={['#4a7c59', '#39656d', '#a25d00', '#ba1a1a'][index % 4]} strokeWidth={3} type="monotone" />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
