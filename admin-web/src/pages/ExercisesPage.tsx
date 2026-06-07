import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { Edit, Eye, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { z } from 'zod';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataTable, type DataColumn } from '../components/DataTable';
import { DrawerForm } from '../components/DrawerForm';
import { FilterBar } from '../components/FilterBar';
import { FormField } from '../components/FormField';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { compactParams, del, get, patch, post } from '../lib/api';
import { getAdminExerciseById } from '../lib/adminResources';
import { boolFromSelect, formatDate, formatNumber, getItems, toNumberOrUndefined } from '../lib/format';
import type { ApiErrorShape, Exercise, Paginated } from '../types';

const exerciseSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên bài tập'),
  description: z.string().optional(),
  instructions: z.string().optional(),
  exerciseType: z.enum(['GYM', 'SPORT', 'CARDIO']),
  category: z.string().optional(),
  muscleGroup: z.string().optional(),
  difficultyLevel: z.enum(['BEGINNER', 'INTERMEDIATE', 'ADVANCED']).optional(),
  metValue: z.number().min(0).optional(),
  videoUrl: z.string().url().or(z.literal('')).optional(),
  imageAvtUrl: z.string().url().or(z.literal('')).optional(),
  secondaryMuscleGroups: z.array(z.string()).optional(),
  equipment: z.string().optional(),
  formTips: z.string().optional(),
  isActive: z.boolean().optional(),
  defaultSets: z.number().min(1).optional(),
  defaultReps: z.number().min(1).optional(),
  defaultWeightKg: z.number().min(0).optional(),
  targetMuscleGroup: z.string().optional(),
  restTimeSeconds: z.number().min(0).optional(),
  defaultDurationMinutes: z.number().min(1).optional(),
  defaultIntensityLevel: z.enum(['LOW', 'MEDIUM', 'HIGH']).optional(),
  movementType: z.string().optional(),
  estimatedCaloriesPerMinute: z.number().min(0).optional(),
});

type ExerciseFilters = {
  search: string;
  exerciseType: string;
  category: string;
  muscleGroup: string;
  difficultyLevel: string;
  isActive: string;
};

const initialFilters: ExerciseFilters = {
  search: '',
  exerciseType: '',
  category: '',
  muscleGroup: '',
  difficultyLevel: '',
  isActive: '',
};

export function ExercisesPage() {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useState(initialFilters);
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState<{ mode: 'create' | 'edit'; exercise?: Exercise } | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Exercise | null>(null);
  const [formError, setFormError] = useState('');

  const params = useMemo(() => compactParams({ page, limit: 20, ...filters }), [filters, page]);
  const exercisesQuery = useQuery({
    queryKey: ['exercises', params],
    queryFn: () => get<Paginated<Exercise>>('/admin/exercises', { params }),
  });
  const editExerciseId = searchParams.get('edit');
  const editExerciseQuery = useQuery({
    queryKey: ['exercises', editExerciseId],
    queryFn: () => getAdminExerciseById(editExerciseId!),
    enabled: Boolean(editExerciseId),
  });

  useEffect(() => {
    if (!editExerciseQuery.data) return;
    setDrawer({ mode: 'edit', exercise: editExerciseQuery.data });
    setSearchParams({}, { replace: true });
  }, [editExerciseQuery.data, setSearchParams]);

  const createExercise = useMutation({
    mutationFn: (payload: Record<string, unknown>) => post<Exercise>('/admin/exercises', payload),
    onSuccess: () => closeAndRefresh(),
  });
  const updateExercise = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) => patch<Exercise>(`/admin/exercises/${id}`, payload),
    onSuccess: () => closeAndRefresh(),
  });
  const deleteExercise = useMutation({
    mutationFn: (id: string) => del(`/admin/exercises/${id}`),
    onSuccess: () => {
      setDeleteTarget(null);
      queryClient.invalidateQueries({ queryKey: ['exercises'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
    },
  });

  function closeAndRefresh() {
    setDrawer(null);
    setFormError('');
    queryClient.invalidateQueries({ queryKey: ['exercises'] });
    queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
  }

  function submitExerciseForm(formData: FormData) {
    const secondaryMuscleGroups = String(formData.get('secondaryMuscleGroups') ?? '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
    const intensity = String(formData.get('defaultIntensityLevel') ?? '');
    const raw = {
      name: String(formData.get('name') ?? '').trim(),
      description: String(formData.get('description') ?? '').trim(),
      instructions: String(formData.get('instructions') ?? '').trim(),
      exerciseType: String(formData.get('exerciseType') ?? 'SPORT') as 'GYM' | 'SPORT' | 'CARDIO',
      category: String(formData.get('category') ?? '').trim(),
      muscleGroup: String(formData.get('muscleGroup') ?? '').trim(),
      difficultyLevel: String(formData.get('difficultyLevel') ?? 'BEGINNER') as 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED',
      metValue: toNumberOrUndefined(formData.get('metValue')),
      videoUrl: String(formData.get('videoUrl') ?? '').trim(),
      imageAvtUrl: String(formData.get('imageAvtUrl') ?? '').trim(),
      secondaryMuscleGroups: secondaryMuscleGroups.length ? secondaryMuscleGroups : undefined,
      equipment: String(formData.get('equipment') ?? '').trim(),
      formTips: String(formData.get('formTips') ?? '').trim(),
      isActive: boolFromSelect(String(formData.get('isActive') ?? 'true')),
      defaultSets: toNumberOrUndefined(formData.get('defaultSets')),
      defaultReps: toNumberOrUndefined(formData.get('defaultReps')),
      defaultWeightKg: toNumberOrUndefined(formData.get('defaultWeightKg')),
      targetMuscleGroup: String(formData.get('targetMuscleGroup') ?? '').trim(),
      restTimeSeconds: toNumberOrUndefined(formData.get('restTimeSeconds')),
      defaultDurationMinutes: toNumberOrUndefined(formData.get('defaultDurationMinutes')),
      defaultIntensityLevel: intensity ? (intensity as 'LOW' | 'MEDIUM' | 'HIGH') : undefined,
      movementType: String(formData.get('movementType') ?? '').trim(),
      estimatedCaloriesPerMinute: toNumberOrUndefined(formData.get('estimatedCaloriesPerMinute')),
    };
    const parsed = exerciseSchema.safeParse(raw);
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu bài tập không hợp lệ');
      return;
    }
    const payload = Object.fromEntries(Object.entries(parsed.data).filter(([, value]) => value !== '' && value !== undefined));
    if (drawer?.mode === 'edit' && drawer.exercise) updateExercise.mutate({ id: drawer.exercise.id, payload });
    else createExercise.mutate(payload);
  }

  const columns: DataColumn<Exercise>[] = [
    {
      key: 'name',
      header: 'Bài tập',
      render: (exercise) => (
        <div className="min-w-[180px]">
          <Link className="font-extrabold text-text hover:text-primary" to={`/exercises/${exercise.id}`}>{exercise.name}</Link>
          <p className="truncate text-xs text-muted">{exercise.category || exercise.muscleGroup || '-'}</p>
        </div>
      ),
    },
    { key: 'type', header: 'Loại', render: (exercise) => <StatusBadge variant="info">{exercise.exerciseType ?? exercise.exercise_type}</StatusBadge> },
    { key: 'difficulty', header: 'Độ khó', render: (exercise) => <StatusBadge variant="neutral">{exercise.difficultyLevel ?? '-'}</StatusBadge> },
    { key: 'met', header: 'MET', render: (exercise) => <span className="font-mono text-xs">{formatNumber(exercise.metValue)}</span> },
    {
      key: 'active',
      header: 'Active',
      render: (exercise) => <StatusBadge variant={statusVariant(exercise.isActive)}>{exercise.isActive ? 'Active' : 'Inactive'}</StatusBadge>,
    },
    { key: 'created', header: 'Ngày tạo', render: (exercise) => <span className="font-mono text-xs">{formatDate(exercise.createdAt)}</span> },
    {
      key: 'actions',
      header: 'Thao tác',
      className: 'text-right',
      render: (exercise) => (
        <div className="flex justify-end gap-1">
          <Link className="icon-btn" to={`/exercises/${exercise.id}`}>
            <Eye className="h-4 w-4" />
          </Link>
          <button className="icon-btn" onClick={() => setDrawer({ mode: 'edit', exercise })} type="button">
            <Edit className="h-4 w-4" />
          </button>
          <button className="icon-btn text-danger hover:text-danger" onClick={() => setDeleteTarget(exercise)} type="button">
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="page-title">Bài tập</h1>
          <p className="mt-1 text-sm text-muted">Quản lý bài tập SPORT, GYM, CARDIO và trạng thái hiển thị</p>
        </div>
        <button className="btn-primary" onClick={() => setDrawer({ mode: 'create' })} type="button">
          <Plus className="h-4 w-4" />
          Thêm bài tập
        </button>
      </div>

      <FilterBar
        actions={
          <button className="btn-secondary" onClick={() => setFilters(initialFilters)} type="button">
            <RotateCcw className="h-4 w-4" />
            Đặt lại
          </button>
        }
      >
        <input className="input" placeholder="Tìm tên bài tập" value={filters.search} onChange={(event) => setFilters({ ...filters, search: event.target.value })} />
        <select className="select" value={filters.exerciseType} onChange={(event) => setFilters({ ...filters, exerciseType: event.target.value })}>
          <option value="">Tất cả loại</option>
          <option value="SPORT">SPORT</option>
          <option value="GYM">GYM</option>
          <option value="CARDIO">CARDIO</option>
        </select>
        <input className="input" placeholder="Danh mục" value={filters.category} onChange={(event) => setFilters({ ...filters, category: event.target.value })} />
        <input className="input" placeholder="Nhóm cơ" value={filters.muscleGroup} onChange={(event) => setFilters({ ...filters, muscleGroup: event.target.value })} />
        <select className="select" value={filters.difficultyLevel} onChange={(event) => setFilters({ ...filters, difficultyLevel: event.target.value })}>
          <option value="">Tất cả độ khó</option>
          <option value="BEGINNER">BEGINNER</option>
          <option value="INTERMEDIATE">INTERMEDIATE</option>
          <option value="ADVANCED">ADVANCED</option>
        </select>
      </FilterBar>

      <DataTable
        columns={columns}
        data={getItems(exercisesQuery.data)}
        emptyTitle="Không có bài tập phù hợp"
        error={(exercisesQuery.error as ApiErrorShape | null)?.message}
        getRowId={(exercise) => exercise.id}
        isLoading={exercisesQuery.isLoading}
        limit={exercisesQuery.data?.limit}
        onPageChange={setPage}
        page={exercisesQuery.data?.page ?? page}
        total={exercisesQuery.data?.total}
      />

      <DrawerForm
        footer={
          <div className="flex justify-end gap-2">
            <button className="btn-secondary" onClick={() => setDrawer(null)} type="button">Hủy</button>
            <button className="btn-primary" disabled={createExercise.isPending || updateExercise.isPending} form="exercise-form" type="submit">
              {drawer?.mode === 'edit' ? 'Lưu thay đổi' : 'Tạo bài tập'}
            </button>
          </div>
        }
        onClose={() => setDrawer(null)}
        open={Boolean(drawer)}
        title={drawer?.mode === 'edit' ? 'Cập nhật bài tập' : 'Thêm bài tập'}
      >
        <form
          className="grid gap-4"
          id="exercise-form"
          onSubmit={(event) => {
            event.preventDefault();
            submitExerciseForm(new FormData(event.currentTarget));
          }}
        >
          <ExerciseFormFields exercise={drawer?.exercise} />
          {formError || createExercise.error || updateExercise.error ? (
            <div className="rounded-md border border-danger/25 bg-danger-soft/40 px-3 py-2 text-sm font-semibold text-danger">
              {formError || (createExercise.error as ApiErrorShape | null)?.message || (updateExercise.error as ApiErrorShape | null)?.message}
            </div>
          ) : null}
        </form>
      </DrawerForm>

      <ConfirmDialog
        confirmLabel="Xóa mềm"
        description={deleteTarget ? `${deleteTarget.name} sẽ bị chuyển sang inactive.` : undefined}
        isLoading={deleteExercise.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteExercise.mutate(deleteTarget.id)}
        open={Boolean(deleteTarget)}
        title="Xóa mềm bài tập?"
      />
    </div>
  );
}

function ExerciseFormFields({ exercise }: { exercise?: Exercise }) {
  const [exerciseType, setExerciseType] = useState(String(exercise?.exerciseType ?? exercise?.exercise_type ?? 'SPORT'));
  return (
    <>
      <FormField label="Tên bài tập"><input className="input" defaultValue={exercise?.name} name="name" /></FormField>
      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Loại">
          <select className="select" name="exerciseType" value={exerciseType} onChange={(event) => setExerciseType(event.target.value)}>
            <option value="SPORT">SPORT</option>
            <option value="GYM">GYM</option>
            <option value="CARDIO">CARDIO</option>
          </select>
        </FormField>
        <FormField label="Danh mục"><input className="input" defaultValue={exercise?.category ?? ''} name="category" /></FormField>
        <FormField label="Độ khó">
          <select className="select" defaultValue={exercise?.difficultyLevel ?? 'BEGINNER'} name="difficultyLevel">
            <option value="BEGINNER">BEGINNER</option>
            <option value="INTERMEDIATE">INTERMEDIATE</option>
            <option value="ADVANCED">ADVANCED</option>
          </select>
        </FormField>
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Nhóm cơ"><input className="input" defaultValue={exercise?.muscleGroup ?? ''} name="muscleGroup" /></FormField>
        <FormField label="MET"><input className="input" defaultValue={exercise?.metValue ?? 0} min="0" name="metValue" type="number" /></FormField>
        <FormField label="Active">
          <select className="select" defaultValue={String(exercise?.isActive ?? true)} name="isActive">
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </FormField>
      </div>
      <FormField label="Mô tả"><textarea className="textarea" defaultValue={exercise?.description ?? ''} name="description" /></FormField>
      <FormField label="Hướng dẫn"><textarea className="textarea" defaultValue={exercise?.instructions ?? ''} name="instructions" /></FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Video URL"><input className="input" defaultValue={exercise?.videoUrl ?? ''} name="videoUrl" /></FormField>
        <FormField label="Image URL"><input className="input" defaultValue={exercise?.imageAvtUrl ?? ''} name="imageAvtUrl" /></FormField>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Thiết bị"><input className="input" defaultValue={exercise?.equipment ?? ''} name="equipment" /></FormField>
        <FormField label="Nhóm cơ phụ"><input className="input" defaultValue={exercise?.secondaryMuscleGroups?.join(', ') ?? ''} name="secondaryMuscleGroups" /></FormField>
      </div>
      <FormField label="Form tips"><textarea className="textarea" defaultValue={exercise?.formTips ?? ''} name="formTips" /></FormField>

      {exerciseType === 'GYM' ? (
        <section className="rounded-lg border border-border bg-surface-low p-4">
          <h3 className="mb-3 text-sm font-extrabold text-text">Thông số GYM</h3>
          <div className="grid gap-4 sm:grid-cols-3">
            <FormField label="Sets"><input className="input" defaultValue={exercise?.defaultSets ?? ''} min="1" name="defaultSets" type="number" /></FormField>
            <FormField label="Reps"><input className="input" defaultValue={exercise?.defaultReps ?? ''} min="1" name="defaultReps" type="number" /></FormField>
            <FormField label="Weight kg"><input className="input" defaultValue={exercise?.defaultWeightKg ?? ''} min="0" name="defaultWeightKg" type="number" /></FormField>
            <FormField label="Target muscle"><input className="input" defaultValue={exercise?.targetMuscleGroup ?? ''} name="targetMuscleGroup" /></FormField>
            <FormField label="Rest seconds"><input className="input" defaultValue={exercise?.restTimeSeconds ?? ''} min="0" name="restTimeSeconds" type="number" /></FormField>
          </div>
        </section>
      ) : (
        <section className="rounded-lg border border-border bg-surface-low p-4">
          <h3 className="mb-3 text-sm font-extrabold text-text">Thông số SPORT/CARDIO</h3>
          <div className="grid gap-4 sm:grid-cols-3">
            <FormField label="Duration phút"><input className="input" defaultValue={exercise?.defaultDurationMinutes ?? ''} min="1" name="defaultDurationMinutes" type="number" /></FormField>
            <FormField label="Intensity">
              <select className="select" defaultValue={exercise?.defaultIntensityLevel ?? ''} name="defaultIntensityLevel">
                <option value="">Không đặt</option>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
              </select>
            </FormField>
            <FormField label="Movement"><input className="input" defaultValue={exercise?.movementType ?? ''} name="movementType" /></FormField>
            <FormField label="Calories/phút"><input className="input" defaultValue={exercise?.estimatedCaloriesPerMinute ?? ''} min="0" name="estimatedCaloriesPerMinute" type="number" /></FormField>
          </div>
        </section>
      )}
    </>
  );
}
