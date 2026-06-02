import { Food } from './entities/food.entity';

/**
 * Convert an AI-estimated physical volume (cm³) into an edible weight (grams)
 * and scale a matched food's per-100g nutrition to that weight.
 *
 * The AI service only returns volume, so we approximate mass with a food
 * density (g/cm³). There is no density data in the DB yet, so we use a small
 * per-category lookup (substring match on `Food.category`) plus a water-like
 * default. Densities are rough averages — accuracy is bounded by the volume
 * estimate itself.
 */

export const DEFAULT_DENSITY_G_PER_CM3 = 1.0;

// Keys are lowercased substrings tested against the food's category.
// First match wins, so order from most to least specific.
export const DENSITY_BY_CATEGORY: Array<[string, number]> = [
  ['soup', 1.0],
  ['broth', 1.0],
  ['beverage', 1.0],
  ['drink', 1.0],
  ['juice', 1.04],
  ['dairy', 1.03],
  ['milk', 1.03],
  ['meat', 1.05],
  ['poultry', 1.05],
  ['fish', 1.05],
  ['seafood', 1.05],
  ['sausage', 1.0],
  ['rice', 0.75],
  ['grain', 0.7],
  ['pasta', 0.7],
  ['cereal', 0.5],
  ['noodle', 0.7],
  ['vegetable', 0.6],
  ['fruit', 0.6],
  ['legume', 0.8],
  ['bean', 0.8],
  ['bread', 0.3],
  ['bun', 0.3],
  ['bakery', 0.4],
  ['snack', 0.4],
  ['fried', 0.4],
  ['fast food', 0.45],
  ['sweet', 0.9],
  ['dessert', 0.9],
];

export function densityForCategory(category?: string | null): number {
  if (!category) return DEFAULT_DENSITY_G_PER_CM3;
  const c = category.toLowerCase();
  for (const [key, density] of DENSITY_BY_CATEGORY) {
    if (c.includes(key)) return density;
  }
  return DEFAULT_DENSITY_G_PER_CM3;
}

/** volume (cm³) × density (g/cm³) → grams, rounded to 1 decimal. */
export function volumeToGrams(
  volumeCm3: number,
  category?: string | null,
): number {
  const grams = volumeCm3 * densityForCategory(category);
  return Math.round(grams * 10) / 10;
}

export interface ScaledNutrition {
  calories: number;
  protein: number;
  fat: number;
  carbs: number;
  fiber: number | null;
}

/**
 * Scale a food's per-100g macros to `grams`. TypeORM returns `decimal`
 * columns as strings, so every value is coerced with Number() first.
 */
export function scaleNutrition(food: Food, grams: number): ScaledNutrition {
  const factor = grams / 100;
  const round = (v: number) => Math.round(v * 10) / 10;
  const num = (v: unknown) => Number(v) || 0;
  const fiberRaw = food.fiber_per_100g;
  return {
    calories: round(num(food.calories_per_100g) * factor),
    protein: round(num(food.protein_per_100g) * factor),
    fat: round(num(food.fat_per_100g) * factor),
    carbs: round(num(food.carbs_per_100g) * factor),
    fiber:
      fiberRaw === null || fiberRaw === undefined
        ? null
        : round(num(fiberRaw) * factor),
  };
}
