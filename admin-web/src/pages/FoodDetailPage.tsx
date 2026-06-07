import { useMemo, useState, type ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  ArrowLeft,
  Check,
  Copy,
  Edit,
  Flame,
  Image as ImageIcon,
  Leaf,
  Scale,
  Star,
  Tag,
  Trash2,
} from 'lucide-react';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { del, patch } from '../lib/api';
import { getAdminFoodById } from '../lib/adminResources';
import {
  formatDate,
  formatDecimal,
  formatNumber,
  getFoodCalories,
  getFoodFavorites,
  getFoodImageUrls,
  getFoodMacro,
  getFoodType,
  isFoodActive,
  isFoodCustom,
  isFoodVerified,
} from '../lib/format';
import type { ApiErrorShape, Food } from '../types';

export function FoodDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [deleteTarget, setDeleteTarget] = useState<Food | null>(null);

  const foodQuery = useQuery({
    queryKey: ['foods', id],
    queryFn: () => getAdminFoodById(id!),
    enabled: Boolean(id),
  });

  const verifyFood = useMutation({
    mutationFn: (foodId: string) => patch<Food>(`/admin/foods/${foodId}/verify`),
    onSuccess: (food) => {
      queryClient.setQueryData(['foods', food.id], food);
      queryClient.invalidateQueries({ queryKey: ['foods'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
    },
  });

  const deleteFood = useMutation({
    mutationFn: (foodId: string) => del(`/admin/foods/${foodId}`),
    onSuccess: () => {
      setDeleteTarget(null);
      queryClient.invalidateQueries({ queryKey: ['foods'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
      navigate('/foods');
    },
  });

  const food = foodQuery.data;
  const imageUrls = useMemo(() => (food ? getFoodImageUrls(food) : []), [food]);
  const heroImage = imageUrls[0];

  if (foodQuery.isLoading) return <LoadingState label="Đang tải thực phẩm" />;
  if (foodQuery.error || !food) {
    return <ErrorState message={(foodQuery.error as ApiErrorShape | null)?.message ?? 'Không tìm thấy thực phẩm'} />;
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link className="btn-secondary" to="/foods">
          <ArrowLeft className="h-4 w-4" />
          Quay lại
        </Link>
        <div className="flex flex-wrap gap-2">
          <Link className="btn-secondary" to={`/foods?edit=${food.id}`}>
            <Edit className="h-4 w-4" />
            Sửa
          </Link>
          {!isFoodVerified(food) ? (
            <button className="btn-primary" disabled={verifyFood.isPending} onClick={() => verifyFood.mutate(food.id)} type="button">
              <Check className="h-4 w-4" />
              Duyệt
            </button>
          ) : null}
          <button className="btn-danger" onClick={() => setDeleteTarget(food)} type="button">
            <Trash2 className="h-4 w-4" />
            Xóa mềm
          </button>
        </div>
      </div>

      <section className="card overflow-hidden">
        <div className="grid gap-0 lg:grid-cols-[minmax(0,1.2fr)_minmax(360px,0.8fr)]">
          <div className="relative min-h-[320px] bg-primary-faint">
            {heroImage ? (
              <img alt={food.name} className="h-full min-h-[320px] w-full object-cover" src={heroImage} />
            ) : (
              <div className="grid h-full min-h-[320px] place-items-center bg-gradient-to-br from-primary-faint to-surface-low">
                <div className="grid h-28 w-28 place-items-center rounded-full bg-primary text-4xl font-extrabold text-white">
                  {food.name.slice(0, 2).toUpperCase()}
                </div>
              </div>
            )}
            <div className="absolute left-5 top-5 flex flex-wrap gap-2">
              <StatusBadge variant={statusVariant(isFoodVerified(food))}>{isFoodVerified(food) ? 'Đã duyệt' : 'Chờ duyệt'}</StatusBadge>
              <StatusBadge variant={statusVariant(isFoodActive(food))}>{isFoodActive(food) ? 'Active' : 'Inactive'}</StatusBadge>
            </div>
          </div>
          <div className="flex flex-col justify-between gap-6 p-6">
            <div>
              <div className="mb-3 flex flex-wrap gap-2">
                <StatusBadge variant="info">{getFoodType(food)}</StatusBadge>
                <StatusBadge variant={isFoodCustom(food) ? 'warning' : 'neutral'}>{isFoodCustom(food) ? 'Custom food' : 'Global food'}</StatusBadge>
              </div>
              <h1 className="text-3xl font-extrabold leading-tight text-text">{food.name}</h1>
              <p className="mt-2 text-sm font-semibold text-muted">{food.name_en ?? food.nameEn ?? food.category ?? 'Không có tên phụ'}</p>
              <p className="mt-5 whitespace-pre-line text-sm leading-6 text-text">{food.description || 'Chưa có mô tả cho thực phẩm này.'}</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <MiniMeta icon={<Tag className="h-4 w-4" />} label="Danh mục" value={food.category || '-'} />
              <MiniMeta icon={<Star className="h-4 w-4" />} label="Brand" value={food.brand || '-'} />
              <MiniMeta icon={<Scale className="h-4 w-4" />} label="Khẩu phần" value={`${formatDecimal(food.serving_size_g ?? food.servingSizeG, ' g')} · ${food.serving_unit ?? food.servingUnit ?? 'g'}`} />
              <MiniMeta icon={<Leaf className="h-4 w-4" />} label="Yêu thích" value={formatNumber(getFoodFavorites(food))} />
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
        <MetricCard icon={<Flame className="h-5 w-5" />} label="Calories" value={formatNumber(getFoodCalories(food), ' kcal')} />
        <MetricCard label="Protein" value={formatDecimal(getFoodMacro(food, 'protein'), ' g')} />
        <MetricCard label="Carbs" value={formatDecimal(getFoodMacro(food, 'carbs'), ' g')} />
        <MetricCard label="Fat" value={formatDecimal(getFoodMacro(food, 'fat'), ' g')} />
        <MetricCard label="Fiber" value={formatDecimal(getFoodMacro(food, 'fiber'), ' g')} />
        <MetricCard label="Serving" value={formatDecimal(food.serving_size_g ?? food.servingSizeG, ' g')} />
      </section>

      <section className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_380px]">
        <div className="card p-5">
          <div className="mb-4 flex items-center gap-2">
            <ImageIcon className="h-5 w-5 text-primary" />
            <h2 className="text-lg font-extrabold text-text">Image URLs</h2>
          </div>
          {imageUrls.length ? (
            <div className="space-y-3">
              {imageUrls.map((url) => (
                <div className="flex min-w-0 items-center gap-3 rounded-lg border border-border bg-surface-low p-3" key={url}>
                  <img alt="" className="h-14 w-14 rounded-md object-cover" src={url} />
                  <a className="min-w-0 flex-1 truncate font-mono text-xs text-primary hover:underline" href={url} rel="noreferrer" target="_blank">
                    {url}
                  </a>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted">Chưa có ảnh cho thực phẩm này.</p>
          )}
        </div>
        <div className="card p-5">
          <h2 className="mb-4 text-lg font-extrabold text-text">Thông tin hệ thống</h2>
          <div className="space-y-3">
            <DetailRow label="ID" mono value={food.id} />
            <DetailRow label="Created by" mono value={food.created_by_user_id ?? food.createdByUserId ?? 'System'} />
            <DetailRow label="Ngày tạo" value={formatDate(food.created_at ?? food.createdAt)} />
            <DetailRow label="Cập nhật" value={formatDate(food.updated_at ?? food.updatedAt)} />
            <DetailRow label="Public IDs" value={(food.image_public_ids ?? food.imagePublicIds ?? []).join(', ') || '-'} />
          </div>
        </div>
      </section>

      <ConfirmDialog
        confirmLabel="Xóa mềm"
        description={`${food.name} sẽ bị chuyển sang inactive và rời khỏi danh sách hiển thị.`}
        isLoading={deleteFood.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget && deleteFood.mutate(deleteTarget.id)}
        open={Boolean(deleteTarget)}
        title="Xóa mềm thực phẩm?"
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
      <p className="mt-1 text-xs font-semibold text-muted">trên 100g</p>
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

function DetailRow({ label, value, mono }: { label: string; value?: string | number | null; mono?: boolean }) {
  return (
    <div className="rounded-md border border-border bg-surface-low px-3 py-2">
      <p className="label mb-1">{label}</p>
      <p className={`break-words text-sm font-bold text-text ${mono ? 'font-mono text-xs' : ''}`}>
        {value ?? '-'}
        {mono && value ? <Copy className="ml-2 inline h-3.5 w-3.5 text-muted" /> : null}
      </p>
    </div>
  );
}
