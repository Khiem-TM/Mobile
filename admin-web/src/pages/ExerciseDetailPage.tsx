import { useMemo, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Clock,
  Dumbbell,
  Edit,
  Flame,
  Gauge,
  HeartPulse,
  Image as ImageIcon,
  PlayCircle,
  Repeat,
  ShieldCheck,
  Timer,
  Trash2,
} from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { del } from '../lib/api';
import { getAdminExerciseById } from '../lib/adminResources';
import {
  formatDate,
  formatDecimal,
  formatNumber,
  getExerciseFavorites,
  getExerciseImageUrls,
  getExerciseType,
  isExerciseActive,
} from '../lib/format';
import type { ApiErrorShape, Exercise } from '../types';

export function ExerciseDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<Exercise | null>(null);

  const exerciseQuery = useQuery({
    queryKey: ['exercises', id],
    queryFn: () => getAdminExerciseById(id!),
    enabled: Boolean(id),
  });

  const deleteExercise = useMutation({
    mutationFn: (exerciseId: string) => del(`/admin/exercises/${exerciseId}`),
    onSuccess: () => {
      setDeleteTarget(null);
      queryClient.invalidateQueries({ queryKey: ['exercises'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      navigate('/exercises');
    },
  });

  const exercise = exerciseQuery.data;
  const imageUrls = useMemo(() => (exercise ? getExerciseImageUrls(exercise) : []), [exercise]);
  const heroImage = imageUrls[0];
  const exerciseType = exercise ? getExerciseType(exercise) : 'SPORT';

  if (exerciseQuery.isLoading) return <LoadingState label="Đang tải bài tập" />;
  if (exerciseQuery.error || !exercise) {
    return <ErrorState message={(exerciseQuery.error as ApiErrorShape | null)?.message ?? 'Không tìm thấy bài tập'} />;
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link className="btn-secondary" to="/exercises">
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </Link>
        <div className="flex flex-wrap gap-2">
          <Link className="btn-secondary" to={`/exercises?edit=${exercise.id}`}>
            <Edit className="h-4 w-4" />
            Sửa
          </Link>
          <button className="btn-danger" onClick={() => setDeleteTarget(exercise)} type="button">
            <Trash2 className="h-4 w-4" />
            Xóa mềm
          </button>
        </div>
      </div>

      <section className="card overflow-hidden">
        <div className="grid lg:grid-cols-[minmax(0,1.05fr)_minmax(380px,0.95fr)]">
          <div className="relative min-h-[340px] bg-primary-faint">
            {heroImage ? (
              <img alt={exercise.name} className="h-full min-h-[340px] w-full object-cover" src={heroImage} />
            ) : (
              <div className="grid h-full min-h-[340px] place-items-center bg-gradient-to-br from-primary-faint to-surface-low">
                <div className="grid h-28 w-28 place-items-center rounded-full bg-primary text-white">
                  <Dumbbell className="h-12 w-12" />
                </div>
              </div>
            )}
            <div className="absolute left-5 top-5 flex flex-wrap gap-2">
              <StatusBadge variant="info">{exerciseType}</StatusBadge>
              <StatusBadge variant={statusVariant(isExerciseActive(exercise))}>{isExerciseActive(exercise) ? 'Active' : 'Inactive'}</StatusBadge>
            </div>
          </div>
          <div className="flex flex-col gap-6 p-6">
            <div>
              <div className="mb-3 flex flex-wrap gap-2">
                <StatusBadge variant="neutral">{exercise.difficultyLevel ?? 'BEGINNER'}</StatusBadge>
                <StatusBadge variant="neutral">{exercise.category || 'Không danh mục'}</StatusBadge>
              </div>
              <h1 className="text-3xl font-extrabold leading-tight text-text">{exercise.name}</h1>
              <p className="mt-2 text-sm font-semibold text-muted">{exercise.muscleGroup || exercise.targetMuscleGroup || 'Chưa đặt nhóm cơ'}</p>
              <p className="mt-5 whitespace-pre-line text-sm leading-6 text-text">{exercise.description || 'Chưa có mô tả cho bài tập này.'}</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <MiniMeta icon={<Dumbbell className="h-4 w-4" />} label="Thiết bị" value={exercise.equipment || 'Không yêu cầu'} />
              <MiniMeta icon={<HeartPulse className="h-4 w-4" />} label="Nhóm cơ phụ" value={exercise.secondaryMuscleGroups?.join(', ') || '-'} />
              <MiniMeta icon={<ShieldCheck className="h-4 w-4" />} label="Movement" value={exercise.movementType || '-'} />
              <MiniMeta icon={<Flame className="h-4 w-4" />} label="Yêu thích" value={formatNumber(getExerciseFavorites(exercise))} />
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <MetricCard icon={<Gauge className="h-5 w-5" />} label="MET" value={formatDecimal(exercise.metValue, '', 2)} />
        <MetricCard icon={<Flame className="h-5 w-5" />} label="Calories/phút" value={formatDecimal(exercise.estimatedCaloriesPerMinute, ' kcal', 2)} />
        <MetricCard icon={<Clock className="h-5 w-5" />} label="Duration" value={formatNumber(exercise.defaultDurationMinutes, ' phút')} />
        <MetricCard icon={<Timer className="h-5 w-5" />} label="Rest" value={formatNumber(exercise.restTimeSeconds, ' giây')} />
        <MetricCard icon={<Repeat className="h-5 w-5" />} label="Sets/Reps" value={`${exercise.defaultSets ?? '-'} / ${exercise.defaultReps ?? '-'}`} />
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <TextPanel title="Hướng dẫn" value={exercise.instructions} />
        <TextPanel title="Form tips" value={exercise.formTips} />
      </section>

      <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_380px]">
        <div className="card p-5">
          <h2 className="mb-4 text-lg font-extrabold text-text">Thông số chuyên môn</h2>
          <div className="grid gap-3 md:grid-cols-2">
            {exerciseType === 'GYM' ? (
              <>
                <DetailRow label="Default sets" value={exercise.defaultSets} />
                <DetailRow label="Default reps" value={exercise.defaultReps} />
                <DetailRow label="Default weight" value={formatDecimal(exercise.defaultWeightKg, ' kg')} />
                <DetailRow label="Target muscle" value={exercise.targetMuscleGroup} />
                <DetailRow label="Rest seconds" value={exercise.restTimeSeconds} />
              </>
            ) : (
              <>
                <DetailRow label="Duration" value={formatNumber(exercise.defaultDurationMinutes, ' phút')} />
                <DetailRow label="Intensity" value={exercise.defaultIntensityLevel} />
                <DetailRow label="Movement type" value={exercise.movementType} />
                <DetailRow label="Calories/min" value={formatDecimal(exercise.estimatedCaloriesPerMinute, ' kcal', 2)} />
                <DetailRow label="Video URL" value={exercise.videoUrl} />
              </>
            )}
          </div>
          {exercise.videoUrl ? (
            <a className="btn-secondary mt-4" href={exercise.videoUrl} rel="noreferrer" target="_blank">
              <PlayCircle className="h-4 w-4" />
              Mở video
            </a>
          ) : null}
        </div>
        <div className="card p-5">
          <div className="mb-4 flex items-center gap-2">
            <ImageIcon className="h-5 w-5 text-primary" />
            <h2 className="text-lg font-extrabold text-text">Media & hệ thống</h2>
          </div>
          <div className="space-y-3">
            <DetailRow label="ID" mono value={exercise.id} />
            <DetailRow label="Ngày tạo" value={formatDate(exercise.createdAt)} />
            <DetailRow label="Cập nhật" value={formatDate(exercise.updatedAt)} />
            <DetailRow label="Image public IDs" value={(exercise.image_public_ids ?? exercise.imagePublicIds ?? []).join(', ') || '-'} />
          </div>
          <div className="mt-4 space-y-3">
            {imageUrls.map((url) => (
              <a className="block truncate rounded-md border border-border bg-surface-low px-3 py-2 font-mono text-xs text-primary hover:underline" href={url} key={url} rel="noreferrer" target="_blank">
                {url}
              </a>
            ))}
            {!imageUrls.length ? <p className="text-sm text-muted">Chưa có ảnh cho bài tập này.</p> : null}
          </div>
        </div>
      </section>

      <ConfirmDialog
        confirmLabel="Xóa mềm"
        description={`${exercise.name} sẽ bị chuyển sang inactive.`}
        isLoading={deleteExercise.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteExercise.mutate(deleteTarget.id)}
        open={Boolean(deleteTarget)}
        title="Xóa mềm bài tập?"
      />
    </div>
  );
}

function MetricCard({ label, value, icon }: { label: string; value: string; icon?: ReactNode }) {
  return (
    <div className="card p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <p className="label">{label}</p>
        {icon ? <span className="text-primary">{icon}</span> : null}
      </div>
      <p className="text-2xl font-extrabold text-text">{value}</p>
    </div>
  );
}

function MiniMeta({ label, value, icon }: { label: string; value: string; icon: ReactNode }) {
  return (
    <div className="rounded-lg border border-border bg-surface-low p-3">
      <div className="mb-1 flex items-center gap-2 text-primary">
        {icon}
        <span className="text-xs font-extrabold uppercase tracking-wide">{label}</span>
      </div>
      <p className="truncate text-sm font-bold text-text">{value}</p>
    </div>
  );
}

function TextPanel({ title, value }: { title: string; value?: string | null }) {
  return (
    <div className="card p-5">
      <h2 className="mb-3 text-lg font-extrabold text-text">{title}</h2>
      <p className="whitespace-pre-line text-sm leading-6 text-text">{value || 'Chưa có nội dung.'}</p>
    </div>
  );
}

function DetailRow({ label, value, mono }: { label: string; value?: string | number | null; mono?: boolean }) {
  return (
    <div className="rounded-md border border-border bg-surface-low px-3 py-2">
      <p className="label mb-1">{label}</p>
      <p className={`break-words text-sm font-bold text-text ${mono ? 'font-mono text-xs' : ''}`}>{value ?? '-'}</p>
    </div>
  );
}
