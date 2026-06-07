import type { AdminUser, Blog, Exercise, Food, Paginated } from '../types';

export function formatNumber(value: number | string | null | undefined, suffix = '') {
  const numeric = Number(value ?? 0);
  return `${new Intl.NumberFormat('vi-VN', {
    notation: numeric >= 10_000 ? 'compact' : 'standard',
    maximumFractionDigits: numeric >= 10_000 ? 1 : 0,
  }).format(numeric)}${suffix}`;
}

export function formatDecimal(value: number | string | null | undefined, suffix = '', maximumFractionDigits = 1) {
  if (value === undefined || value === null || value === '') return '-';
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) return '-';
  return `${new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits,
  }).format(numeric)}${suffix}`;
}

export function formatDate(value?: string | Date | null) {
  if (!value) return '-';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(date);
}

export function formatShortDate(value?: string | Date | null) {
  if (!value) return '-';
  const date = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(date);
}

export function getUserDisplayName(user?: AdminUser | null) {
  return user?.display_name ?? user?.displayName ?? user?.email ?? '-';
}

export function isUserActive(user: AdminUser) {
  return user.is_active ?? user.isActive ?? false;
}

export function isUserVerified(user: AdminUser) {
  return user.is_verified ?? user.isVerified ?? false;
}

export function getFoodType(food: Food) {
  return food.food_type ?? food.foodType ?? 'ingredient';
}

export function isFoodActive(food: Food) {
  return food.is_active ?? food.isActive ?? false;
}

export function isFoodVerified(food: Food) {
  return food.is_verified ?? food.isVerified ?? false;
}

export function isFoodCustom(food: Food) {
  return food.is_custom ?? food.isCustom ?? false;
}

export function getFoodCalories(food: Food) {
  return Number(food.calories_per_100g ?? food.caloriesPer100g ?? 0);
}

export function getFoodMacro(food: Food, key: 'protein' | 'fat' | 'carbs' | 'fiber') {
  if (key === 'protein') return Number(food.protein_per_100g ?? food.proteinPer100g ?? 0);
  if (key === 'fat') return Number(food.fat_per_100g ?? food.fatPer100g ?? 0);
  if (key === 'carbs') return Number(food.carbs_per_100g ?? food.carbsPer100g ?? 0);
  return Number(food.fiber_per_100g ?? food.fiberPer100g ?? 0);
}

export function getFoodImageUrls(food: Food) {
  return (food.image_urls ?? food.imageUrls ?? []).filter(Boolean);
}

export function getFoodFavorites(food: Food) {
  return Number(food.favorites_count ?? food.favoritesCount ?? 0);
}

export function getExerciseType(exercise: Exercise) {
  return exercise.exerciseType ?? exercise.exercise_type ?? 'SPORT';
}

export function isExerciseActive(exercise: Exercise) {
  return exercise.isActive ?? false;
}

export function getExerciseImageUrls(exercise: Exercise) {
  return [exercise.imageAvtUrl, ...(exercise.imageUrl ?? [])].filter(Boolean) as string[];
}

export function getExerciseFavorites(exercise: Exercise) {
  return Number(exercise.favoritesCount ?? 0);
}

export function getBlogTags(blog: Blog) {
  if (Array.isArray(blog.tags)) return blog.tags;
  if (typeof blog.tags === 'string') return blog.tags.split(',').filter(Boolean);
  return [];
}

export function getItems<T>(data?: Paginated<T> | null): T[] {
  return data?.users ?? data?.foods ?? data?.exercises ?? data?.items ?? [];
}

export function pageCount(total = 0, limit = 20) {
  return Math.max(1, Math.ceil(total / limit));
}

export function toNumberOrUndefined(value: FormDataEntryValue | null) {
  if (value === null || value === '') return undefined;
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : undefined;
}

export function boolFromSelect(value: string) {
  if (value === 'true') return true;
  if (value === 'false') return false;
  return undefined;
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export function daysAgoIso(days: number) {
  const date = new Date();
  date.setDate(date.getDate() - days);
  return date.toISOString().slice(0, 10);
}
