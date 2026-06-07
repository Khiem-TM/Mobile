import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { Check, Edit, Eye, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { z } from 'zod';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DataTable, type DataColumn } from '../components/DataTable';
import { DrawerForm } from '../components/DrawerForm';
import { FilterBar } from '../components/FilterBar';
import { FormField } from '../components/FormField';
import { StatusBadge, statusVariant } from '../components/StatusBadge';
import { compactParams, del, get, patch, post } from '../lib/api';
import { getAdminFoodById } from '../lib/adminResources';
import {
  boolFromSelect,
  formatDate,
  formatNumber,
  getFoodCalories,
  getFoodType,
  getItems,
  isFoodActive,
  isFoodVerified,
  toNumberOrUndefined,
} from '../lib/format';
import type { ApiErrorShape, Food, Paginated } from '../types';

const foodSchema = z.object({
  name: z.string().min(1, 'Vui lòng nhập tên thực phẩm'),
  nameEn: z.string().optional(),
  brand: z.string().optional(),
  category: z.string().optional(),
  foodType: z.enum(['ingredient', 'dish', 'product']).optional(),
  servingSizeG: z.number().min(0).optional(),
  servingUnit: z.string().optional(),
  caloriesPer100g: z.number().min(0, 'Calories không hợp lệ'),
  proteinPer100g: z.number().min(0).optional(),
  fatPer100g: z.number().min(0).optional(),
  carbsPer100g: z.number().min(0).optional(),
  fiberPer100g: z.number().min(0).optional(),
  description: z.string().optional(),
  imageUrls: z.array(z.string().url()).optional(),
  isVerified: z.boolean().optional(),
  isActive: z.boolean().optional(),
});

type FoodFilters = {
  search: string;
  foodType: string;
  category: string;
  isVerified: string;
  isActive: string;
  createdFrom: string;
};

const initialFilters: FoodFilters = {
  search: '',
  foodType: '',
  category: '',
  isVerified: '',
  isActive: '',
  createdFrom: '',
};

export function FoodsPage() {
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();
  const [filters, setFilters] = useState(initialFilters);
  const [page, setPage] = useState(1);
  const [drawer, setDrawer] = useState<{ mode: 'create' | 'edit'; food?: Food } | null>(null);
  const [deleteFoodTarget, setDeleteFoodTarget] = useState<Food | null>(null);
  const [formError, setFormError] = useState('');

  const params = useMemo(() => compactParams({ page, limit: 20, ...filters }), [filters, page]);
  const foodsQuery = useQuery({
    queryKey: ['foods', 'all', params],
    queryFn: () => get<Paginated<Food>>('/admin/foods', { params }),
  });
  const editFoodId = searchParams.get('edit');
  const editFoodQuery = useQuery({
    queryKey: ['foods', editFoodId],
    queryFn: () => getAdminFoodById(editFoodId!),
    enabled: Boolean(editFoodId),
  });

  useEffect(() => {
    if (!editFoodQuery.data) return;
    setDrawer({ mode: 'edit', food: editFoodQuery.data });
    setSearchParams({}, { replace: true });
  }, [editFoodQuery.data, setSearchParams]);

  const createFood = useMutation({
    mutationFn: (payload: Record<string, unknown>) => post<Food>('/admin/foods', payload),
    onSuccess: () => closeAndRefresh(),
  });
  const updateFood = useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: Record<string, unknown> }) => patch<Food>(`/admin/foods/${id}`, payload),
    onSuccess: () => closeAndRefresh(),
  });
  const verifyFood = useMutation({
    mutationFn: (id: string) => patch<Food>(`/admin/foods/${id}/verify`),
    onSuccess: () => refreshFoods(),
  });
  const deleteFood = useMutation({
    mutationFn: (id: string) => del(`/admin/foods/${id}`),
    onSuccess: () => {
      setDeleteFoodTarget(null);
      refreshFoods();
    },
  });

  function refreshFoods() {
    queryClient.invalidateQueries({ queryKey: ['foods'] });
    queryClient.invalidateQueries({ queryKey: ['admin-stats'] });
  }

  function closeAndRefresh() {
    setDrawer(null);
    setFormError('');
    refreshFoods();
  }

  function submitFoodForm(formData: FormData) {
    const imageUrls = String(formData.get('imageUrls') ?? '')
      .split(/[\n,]/)
      .map((item) => item.trim())
      .filter(Boolean);
    const raw = {
      name: String(formData.get('name') ?? '').trim(),
      nameEn: String(formData.get('nameEn') ?? '').trim(),
      brand: String(formData.get('brand') ?? '').trim(),
      category: String(formData.get('category') ?? '').trim(),
      foodType: String(formData.get('foodType') ?? 'ingredient') as 'ingredient' | 'dish' | 'product',
      servingSizeG: toNumberOrUndefined(formData.get('servingSizeG')),
      servingUnit: String(formData.get('servingUnit') ?? 'g').trim(),
      caloriesPer100g: Number(formData.get('caloriesPer100g') ?? 0),
      proteinPer100g: toNumberOrUndefined(formData.get('proteinPer100g')),
      fatPer100g: toNumberOrUndefined(formData.get('fatPer100g')),
      carbsPer100g: toNumberOrUndefined(formData.get('carbsPer100g')),
      fiberPer100g: toNumberOrUndefined(formData.get('fiberPer100g')),
      description: String(formData.get('description') ?? '').trim(),
      imageUrls: imageUrls.length ? imageUrls : undefined,
      isVerified: boolFromSelect(String(formData.get('isVerified') ?? 'true')),
      isActive: boolFromSelect(String(formData.get('isActive') ?? 'true')),
    };
    const parsed = foodSchema.safeParse(raw);
    if (!parsed.success) {
      setFormError(parsed.error.issues[0]?.message ?? 'Dữ liệu thực phẩm không hợp lệ');
      return;
    }
    const payload = Object.fromEntries(Object.entries(parsed.data).filter(([, value]) => value !== '' && value !== undefined));
    if (drawer?.mode === 'edit' && drawer.food) updateFood.mutate({ id: drawer.food.id, payload });
    else {
      delete payload.isActive;
      createFood.mutate(payload);
    }
  }

  const columns: DataColumn<Food>[] = [
    {
      key: 'name',
      header: 'Thực phẩm',
      render: (food) => (
        <div className="min-w-[180px]">
          <Link className="font-extrabold text-text hover:text-primary" to={`/foods/${food.id}`}>{food.name}</Link>
          <p className="truncate text-xs text-muted">{food.brand || food.category || 'Global food'}</p>
        </div>
      ),
    },
    { key: 'type', header: 'Loại', render: (food) => <StatusBadge variant="info">{getFoodType(food)}</StatusBadge> },
    { key: 'calories', header: 'Calories', render: (food) => <span className="font-mono text-xs">{formatNumber(getFoodCalories(food), ' kcal')}</span> },
    {
      key: 'verified',
      header: 'Duyệt',
      render: (food) => <StatusBadge variant={statusVariant(isFoodVerified(food))}>{isFoodVerified(food) ? 'Đã duyệt' : 'Chờ duyệt'}</StatusBadge>,
    },
    {
      key: 'active',
      header: 'Active',
      render: (food) => <StatusBadge variant={statusVariant(isFoodActive(food))}>{isFoodActive(food) ? 'Active' : 'Inactive'}</StatusBadge>,
    },
    { key: 'created', header: 'Ngày tạo', render: (food) => <span className="font-mono text-xs">{formatDate(food.created_at ?? food.createdAt)}</span> },
    {
      key: 'actions',
      header: 'Thao tác',
      className: 'text-right',
      render: (food) => (
        <div className="flex justify-end gap-1">
          <Link className="icon-btn" to={`/foods/${food.id}`}>
            <Eye className="h-4 w-4" />
          </Link>
          <button className="icon-btn" onClick={() => setDrawer({ mode: 'edit', food })} type="button">
            <Edit className="h-4 w-4" />
          </button>
          {!isFoodVerified(food) ? (
            <button className="icon-btn text-primary hover:text-primary" onClick={() => verifyFood.mutate(food.id)} type="button">
              <Check className="h-4 w-4" />
            </button>
          ) : null}
          <button className="icon-btn text-danger hover:text-danger" onClick={() => setDeleteFoodTarget(food)} type="button">
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
          <h1 className="page-title">Thực phẩm</h1>
          <p className="mt-1 text-sm text-muted">Quản lý global food của hệ thống; custom food của user là dữ liệu cá nhân</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button className="btn-primary" onClick={() => setDrawer({ mode: 'create' })} type="button">
            <Plus className="h-4 w-4" />
            Thêm thực phẩm
          </button>
        </div>
      </div>

      <FilterBar
        actions={
          <button className="btn-secondary" onClick={() => setFilters(initialFilters)} type="button">
            <RotateCcw className="h-4 w-4" />
            Đặt lại
          </button>
        }
      >
        <input className="input" placeholder="Tìm tên, brand..." value={filters.search} onChange={(event) => setFilters({ ...filters, search: event.target.value })} />
        <select className="select" value={filters.foodType} onChange={(event) => setFilters({ ...filters, foodType: event.target.value })}>
          <option value="">Tất cả loại</option>
          <option value="ingredient">Ingredient</option>
          <option value="dish">Dish</option>
          <option value="product">Product</option>
        </select>
        <input className="input" placeholder="Danh mục" value={filters.category} onChange={(event) => setFilters({ ...filters, category: event.target.value })} />
        <select className="select" value={filters.isVerified} onChange={(event) => setFilters({ ...filters, isVerified: event.target.value })}>
          <option value="">Tất cả duyệt</option>
          <option value="true">Đã duyệt</option>
          <option value="false">Chưa duyệt</option>
        </select>
        <select className="select" value={filters.isActive} onChange={(event) => setFilters({ ...filters, isActive: event.target.value })}>
          <option value="">Tất cả active</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
      </FilterBar>

      <DataTable
        columns={columns}
        data={getItems(foodsQuery.data)}
        emptyTitle="Không có thực phẩm phù hợp"
        error={(foodsQuery.error as ApiErrorShape | null)?.message}
        getRowId={(food) => food.id}
        isLoading={foodsQuery.isLoading}
        limit={foodsQuery.data?.limit}
        onPageChange={setPage}
        page={foodsQuery.data?.page ?? page}
        total={foodsQuery.data?.total}
      />

      <DrawerForm
        footer={
          <div className="flex justify-end gap-2">
            <button className="btn-secondary" onClick={() => setDrawer(null)} type="button">Hủy</button>
            <button className="btn-primary" disabled={createFood.isPending || updateFood.isPending} form="food-form" type="submit">
              {drawer?.mode === 'edit' ? 'Lưu thay đổi' : 'Tạo thực phẩm'}
            </button>
          </div>
        }
        onClose={() => setDrawer(null)}
        open={Boolean(drawer)}
        title={drawer?.mode === 'edit' ? 'Cập nhật thực phẩm' : 'Thêm thực phẩm'}
      >
        <form
          className="grid gap-4"
          id="food-form"
          onSubmit={(event) => {
            event.preventDefault();
            submitFoodForm(new FormData(event.currentTarget));
          }}
        >
          <FoodFormFields food={drawer?.food} />
          {formError || createFood.error || updateFood.error ? (
            <div className="rounded-md border border-danger/25 bg-danger-soft/40 px-3 py-2 text-sm font-semibold text-danger">
              {formError || (createFood.error as ApiErrorShape | null)?.message || (updateFood.error as ApiErrorShape | null)?.message}
            </div>
          ) : null}
        </form>
      </DrawerForm>

      <ConfirmDialog
        confirmLabel="Xóa mềm"
        description={deleteFoodTarget ? `${deleteFoodTarget.name} sẽ bị chuyển sang trạng thái không hoạt động.` : undefined}
        isLoading={deleteFood.isPending}
        onCancel={() => setDeleteFoodTarget(null)}
        onConfirm={() => {
          if (!deleteFoodTarget) return;
          deleteFood.mutate(deleteFoodTarget.id);
        }}
        open={Boolean(deleteFoodTarget)}
        title="Xóa mềm thực phẩm?"
      />
    </div>
  );
}

function FoodFormFields({ food }: { food?: Food }) {
  const imageUrls = food?.image_urls ?? food?.imageUrls ?? [];
  return (
    <>
      <FormField label="Tên thực phẩm"><input className="input" defaultValue={food?.name} name="name" /></FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Tên tiếng Anh"><input className="input" defaultValue={food?.name_en ?? food?.nameEn ?? ''} name="nameEn" /></FormField>
        <FormField label="Brand"><input className="input" defaultValue={food?.brand ?? ''} name="brand" /></FormField>
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        <FormField label="Danh mục"><input className="input" defaultValue={food?.category ?? ''} name="category" /></FormField>
        <FormField label="Loại">
          <select className="select" defaultValue={getFoodType(food ?? ({ food_type: 'ingredient' } as Food))} name="foodType">
            <option value="ingredient">Ingredient</option>
            <option value="dish">Dish</option>
            <option value="product">Product</option>
          </select>
        </FormField>
        <FormField label="Serving unit"><input className="input" defaultValue={food?.serving_unit ?? food?.servingUnit ?? 'g'} name="servingUnit" /></FormField>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Serving size"><input className="input" defaultValue={food?.serving_size_g ?? food?.servingSizeG ?? 100} min="0" name="servingSizeG" type="number" /></FormField>
        <FormField label="Calories / 100g"><input className="input" defaultValue={food ? getFoodCalories(food) : 0} min="0" name="caloriesPer100g" required type="number" /></FormField>
      </div>
      <div className="grid gap-4 sm:grid-cols-4">
        <FormField label="Protein"><input className="input" defaultValue={food?.protein_per_100g ?? food?.proteinPer100g ?? 0} min="0" name="proteinPer100g" type="number" /></FormField>
        <FormField label="Fat"><input className="input" defaultValue={food?.fat_per_100g ?? food?.fatPer100g ?? 0} min="0" name="fatPer100g" type="number" /></FormField>
        <FormField label="Carbs"><input className="input" defaultValue={food?.carbs_per_100g ?? food?.carbsPer100g ?? 0} min="0" name="carbsPer100g" type="number" /></FormField>
        <FormField label="Fiber"><input className="input" defaultValue={food?.fiber_per_100g ?? food?.fiberPer100g ?? ''} min="0" name="fiberPer100g" type="number" /></FormField>
      </div>
      <FormField label="Mô tả"><textarea className="textarea" defaultValue={food?.description ?? ''} name="description" /></FormField>
      <FormField label="Image URLs"><textarea className="textarea" defaultValue={imageUrls.join('\n')} name="imageUrls" /></FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Duyệt">
          <select className="select" defaultValue={String(food ? isFoodVerified(food) : true)} name="isVerified">
            <option value="true">Đã duyệt</option>
            <option value="false">Chờ duyệt</option>
          </select>
        </FormField>
        <FormField label="Active">
          <select className="select" defaultValue={String(food ? isFoodActive(food) : true)} name="isActive">
            <option value="true">Active</option>
            <option value="false">Inactive</option>
          </select>
        </FormField>
      </div>
    </>
  );
}
