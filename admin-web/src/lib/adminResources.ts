import { get } from './api';
import { getItems } from './format';
import type { ApiErrorShape, Exercise, Food, Paginated } from '../types';

const FALLBACK_LIMIT = 100;

export async function getAdminFoodById(id: string): Promise<Food> {
  try {
    return await get<Food>(`/admin/foods/${id}`);
  } catch (error) {
    if (!isMissingRouteError(error, `/admin/foods/${id}`)) throw error;
    return findFoodByIdFromList(id, error);
  }
}

export async function getAdminExerciseById(id: string): Promise<Exercise> {
  try {
    return await get<Exercise>(`/admin/exercises/${id}`);
  } catch (error) {
    if (!isMissingRouteError(error, `/admin/exercises/${id}`)) throw error;
    return findExerciseByIdFromList(id, error);
  }
}

async function findFoodByIdFromList(id: string, originalError: unknown): Promise<Food> {
  let page = 1;
  while (true) {
    const response = await get<Paginated<Food>>('/admin/foods', {
      params: { page, limit: FALLBACK_LIMIT },
    });
    const match = getItems(response).find((food) => food.id === id);
    if (match) return match;
    if (page * response.limit >= response.total) break;
    page += 1;
  }
  throw originalError;
}

async function findExerciseByIdFromList(id: string, originalError: unknown): Promise<Exercise> {
  let page = 1;
  while (true) {
    const response = await get<Paginated<Exercise>>('/admin/exercises', {
      params: { page, limit: FALLBACK_LIMIT },
    });
    const match = getItems(response).find((exercise) => exercise.id === id);
    if (match) return match;
    if (page * response.limit >= response.total) break;
    page += 1;
  }
  throw originalError;
}

function isMissingRouteError(error: unknown, path: string) {
  const apiError = error as ApiErrorShape | undefined;
  return apiError?.statusCode === 404 && apiError.message.includes(`Cannot GET ${path}`);
}
