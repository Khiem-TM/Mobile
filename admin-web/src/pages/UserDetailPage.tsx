import { useState, type ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  Activity,
  ArrowLeft,
  Bot,
  Calendar,
  Droplets,
  Dumbbell,
  FileText,
  Flame,
  Mail,
  Ruler,
  Salad,
  Scale,
  ShieldCheck,
  Target,
  UserRound,
} from 'lucide-react';
import { ChartCard } from '../components/ChartCard';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { get } from '../lib/api';
import { formatDate, formatDecimal, formatNumber, getUserDisplayName, isUserActive, isUserVerified } from '../lib/format';
import type { AdminUser, ApiErrorShape, TrainingSession, UserHealthProfile } from '../types';

const tabs = ['overview', 'health', 'workouts', 'activity'] as const;
type Tab = (typeof tabs)[number];

export function UserDetailPage() {
  const { id } = useParams();
  const [tab, setTab] = useState<Tab>('overview');
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
  const displayName = getUserDisplayName(user);
  const avatarUrl = user.avatar_url ?? user.avatarUrl;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <Link className="btn-secondary" to="/users">
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </Link>
      </div>

      <section className="card overflow-hidden">
        <div className="grid gap-0 lg:grid-cols-[minmax(0,1fr)_360px]">
          <div className="flex flex-col gap-5 p-6 sm:flex-row sm:items-center">
            <div className="grid h-24 w-24 shrink-0 place-items-center overflow-hidden rounded-full bg-primary text-3xl font-extrabold text-white">
              {avatarUrl ? <img alt={displayName} className="h-full w-full object-cover" src={avatarUrl} /> : displayName.slice(0, 1).toUpperCase()}
            </div>
            <div className="min-w-0 flex-1">
              <div className="mb-3 flex flex-wrap gap-2">
                <StatusBadge variant={statusVariant(isUserActive(user))}>{isUserActive(user) ? 'Hoạt động' : 'Đã khóa'}</StatusBadge>
                <StatusBadge variant={statusVariant(isUserVerified(user))}>{isUserVerified(user) ? 'Đã xác minh' : 'Chưa xác minh'}</StatusBadge>
                <StatusBadge variant="info">{user.role}</StatusBadge>
              </div>
              <h1 className="truncate text-3xl font-extrabold text-text">{displayName}</h1>
              <p className="mt-2 flex items-center gap-2 truncate text-sm font-semibold text-muted">
                <Mail className="h-4 w-4 shrink-0" />
                {user.email}
              </p>
              <p className="mt-3 font-mono text-xs text-muted">{user.id}</p>
            </div>
          </div>
          <div className="grid content-center gap-3 border-t border-border bg-surface-low p-6 lg:border-l lg:border-t-0">
            <MiniMeta icon={<Calendar className="h-4 w-4" />} label="Ngày tạo" value={formatDate(user.created_at ?? user.createdAt)} />
            <MiniMeta icon={<Activity className="h-4 w-4" />} label="Cập nhật" value={formatDate(user.updated_at ?? user.updatedAt)} />
          </div>
        </div>
      </section>

      <div className="flex overflow-x-auto rounded-lg border border-border bg-surface p-1">
        {[
          ['overview', 'Tổng quan'],
          ['health', 'Health profile'],
          ['workouts', 'Tập luyện'],
          ['activity', 'Hoạt động'],
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

      {tab === 'overview' ? <OverviewTab user={user} /> : null}
      {tab === 'health' ? <HealthTab profile={user.healthProfile} /> : null}
      {tab === 'workouts' ? <WorkoutsTab sessions={user.recentSessions ?? []} /> : null}
      {tab === 'activity' ? <ActivityTab user={user} /> : null}
    </div>
  );
}

function OverviewTab({ user }: { user: AdminUser }) {
  const summary = user.adminSummary ?? {};
  return (
    <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
      <InfoCard icon={<Salad className="h-5 w-5" />} label="Meal logs" value={formatNumber(summary.mealLogCount)} />
      <InfoCard icon={<Dumbbell className="h-5 w-5" />} label="Training sessions" value={formatNumber(user.recentSessions?.length)} />
      <InfoCard icon={<FileText className="h-5 w-5" />} label="Bài viết" value={formatNumber(summary.blogCount)} />
      <InfoCard icon={<Bot className="h-5 w-5" />} label="AI scans" value={formatNumber(summary.aiScanCount)} />
      <InfoCard icon={<UserRound className="h-5 w-5" />} label="Chat sessions" value={formatNumber(summary.chatSessionCount)} />
    </section>
  );
}

function HealthTab({ profile }: { profile?: UserHealthProfile | null }) {
  if (!profile) {
    return (
      <ChartCard title="Health profile">
        <div className="text-sm text-muted">Người dùng chưa hoàn tất health profile.</div>
      </ChartCard>
    );
  }
  const birthDate = pick(profile.birthDate, profile.birth_date);
  const foodAllergies = pick(profile.foodAllergies, profile.food_allergies) ?? [];
  return (
    <div className="space-y-4">
      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <InfoCard icon={<Calendar className="h-5 w-5" />} label="Ngày sinh / tuổi" value={`${formatDateOnly(birthDate)} · ${ageFromBirthDate(birthDate)}`} />
        <InfoCard icon={<ShieldCheck className="h-5 w-5" />} label="Giới tính" value={profile.gender || '-'} />
        <InfoCard icon={<Ruler className="h-5 w-5" />} label="Chiều cao" value={formatDecimal(pick(profile.heightCm, profile.height_cm), ' cm')} />
        <InfoCard icon={<Scale className="h-5 w-5" />} label="Cân nặng ban đầu" value={formatDecimal(pick(profile.initialWeightKg, profile.initial_weight_kg), ' kg')} />
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <ChartCard title="Mục tiêu sức khỏe">
          <div className="grid gap-3 sm:grid-cols-2">
            <DetailPill label="Goal type" value={pick(profile.goalType, profile.goal_type)} />
            <DetailPill label="Goal status" value={pick(profile.goalStatus, profile.goal_status)} />
            <DetailPill label="Target weight" value={formatDecimal(pick(profile.targetWeightKg, profile.target_weight_kg), ' kg')} />
            <DetailPill label="Weekly rate" value={formatDecimal(pick(profile.weeklyRateKg, profile.weekly_rate_kg), ' kg/tuần')} />
            <DetailPill label="Start date" value={formatDateOnly(pick(profile.goalStartDate, profile.goal_start_date))} />
            <DetailPill label="Deadline" value={formatDateOnly(pick(profile.goalDeadline, profile.goal_deadline))} />
          </div>
        </ChartCard>
        <ChartCard title="Thói quen & giới hạn">
          <div className="grid gap-3 sm:grid-cols-2">
            <DetailPill label="Activity level" value={pick(profile.activityLevel, profile.activity_level)} />
            <DetailPill label="Diet type" value={pick(profile.dietType, profile.diet_type)} />
            <DetailPill label="Food allergies" value={foodAllergies.length ? foodAllergies.join(', ') : 'Không ghi nhận'} />
            <DetailPill label="Step goal" value={formatNumber(pick(profile.stepGoal, profile.step_goal), ' bước')} />
            <DetailPill label="Water goal" value={formatNumber(pick(profile.waterGoalMl, profile.water_goal_ml), ' ml')} />
          </div>
        </ChartCard>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <InfoCard icon={<Flame className="h-5 w-5" />} label="Calories goal" value={formatDecimal(pick(profile.caloriesGoal, profile.calories_goal), ' kcal')} />
        <InfoCard icon={<Target className="h-5 w-5" />} label="Daily calories" value={formatDecimal(pick(profile.dailyCaloriesGoal, profile.daily_calories_goal), ' kcal')} />
        <InfoCard icon={<Activity className="h-5 w-5" />} label="Protein" value={formatDecimal(pick(profile.proteinGoalG, profile.protein_goal_g), ' g')} />
        <InfoCard icon={<Droplets className="h-5 w-5" />} label="Fat / Carbs" value={`${formatDecimal(pick(profile.fatGoalG, profile.fat_goal_g), ' g')} · ${formatDecimal(pick(profile.carbsGoalG, profile.carbs_goal_g), ' g')}`} />
      </section>
    </div>
  );
}

function WorkoutsTab({ sessions }: { sessions: TrainingSession[] }) {
  return (
    <ChartCard title="Buổi tập gần đây">
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead>
            <tr className="border-b border-border text-left text-[11px] font-extrabold uppercase tracking-wide text-muted">
              <th className="px-3 py-2">Thời gian</th>
              <th className="px-3 py-2">Thời lượng</th>
              <th className="px-3 py-2">Calories</th>
              <th className="px-3 py-2">Bài tập</th>
            </tr>
          </thead>
          <tbody>
            {sessions.map((session) => (
              <tr className="border-b border-border last:border-0" key={session.id}>
                <td className="px-3 py-3 font-mono text-xs">{formatDate(session.sessionDate ?? session.createdAt)}</td>
                <td className="px-3 py-3">{formatNumber(session.totalDurationMinutes, ' phút')}</td>
                <td className="px-3 py-3">{formatNumber(session.totalCaloriesBurned, ' kcal')}</td>
                <td className="px-3 py-3">{session.items?.map((item) => item.exercise?.name).filter(Boolean).join(', ') || `${session.items?.length ?? 0} items`}</td>
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
    <section className="grid gap-4 lg:grid-cols-2">
      <ChartCard title="Tổng hợp nội dung">
        <div className="grid gap-3 sm:grid-cols-2">
          <DetailPill label="Blog posts" value={formatNumber(summary.blogCount)} />
          <DetailPill label="AI scans" value={formatNumber(summary.aiScanCount)} />
          <DetailPill label="Chat sessions" value={formatNumber(summary.chatSessionCount)} />
          <DetailPill label="Meal logs" value={formatNumber(summary.mealLogCount)} />
        </div>
      </ChartCard>
      <ChartCard title="Trạng thái tài khoản">
        <div className="grid gap-3 sm:grid-cols-2">
          <DetailPill label="Role" value={user.role} />
          <DetailPill label="Email verified" value={isUserVerified(user) ? 'Đã xác minh' : 'Chưa xác minh'} />
          <DetailPill label="Active" value={isUserActive(user) ? 'Hoạt động' : 'Đã khóa'} />
          <DetailPill label="Updated" value={formatDate(user.updated_at ?? user.updatedAt)} />
        </div>
      </ChartCard>
    </section>
  );
}

function InfoCard({ label, value, icon }: { label: string; value?: string | number; icon?: ReactNode }) {
  return (
    <div className="card p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <p className="label">{label}</p>
        {icon ? <span className="text-primary">{icon}</span> : null}
      </div>
      <p className="break-words text-xl font-extrabold text-text">{value ?? '-'}</p>
    </div>
  );
}

function MiniMeta({ label, value, icon }: { label: string; value: string; icon: ReactNode }) {
  return (
    <div className="rounded-lg border border-border bg-surface p-3">
      <div className="mb-1 flex items-center gap-2 text-primary">
        {icon}
        <span className="text-xs font-extrabold uppercase tracking-wide">{label}</span>
      </div>
      <p className="text-sm font-bold text-text">{value}</p>
    </div>
  );
}

function DetailPill({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <div className="rounded-md border border-border bg-surface-low px-3 py-2">
      <p className="label mb-1">{label}</p>
      <p className="break-words text-sm font-bold text-text">{value || '-'}</p>
    </div>
  );
}

function pick<T>(first: T | undefined, second: T | undefined): T | undefined {
  return first ?? second;
}

function formatDateOnly(value?: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium' }).format(date);
}

function ageFromBirthDate(value?: string | null) {
  if (!value) return '- tuổi';
  const birth = new Date(value);
  if (Number.isNaN(birth.getTime())) return '- tuổi';
  const now = new Date();
  let age = now.getFullYear() - birth.getFullYear();
  const hasHadBirthday =
    now.getMonth() > birth.getMonth() ||
    (now.getMonth() === birth.getMonth() && now.getDate() >= birth.getDate());
  if (!hasHadBirthday) age -= 1;
  return `${Math.max(age, 0)} tuổi`;
}
