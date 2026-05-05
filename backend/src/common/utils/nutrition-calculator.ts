import { FoodIngredient } from '../../modules/food/entities/food-ingredient.entity';

export interface NutritionResult {
  calories: number;
  protein: number;
  fat: number;
  carbs: number;
}

/**
 * Compute total nutrition from a list of food ingredients.
 *
 * For each ingredient:
 * - If ingredient_food_id is present in foodMap: scale Food nutrition by quantity_g / 100
 * - Otherwise: use flat override values provided by the user (calories_override etc.)
 *
 * @param ingredients - List of FoodIngredient records
 * @param foodMap - Map keyed by food ID containing the referenced Food entities
 * @returns Summed nutrition totals (not per-100g)
 */
export function computeNutritionFromIngredients(
  ingredients: FoodIngredient[],
  foodMap: Map<string, { calories_per_100g: number; protein_per_100g: number; fat_per_100g: number; carbs_per_100g: number }>,
): NutritionResult {
  return ingredients.reduce<NutritionResult>(
    (acc, ing) => {
      const ratio = Number(ing.quantity_g) / 100;

      if (ing.ingredient_food_id && foodMap.has(ing.ingredient_food_id)) {
        const food = foodMap.get(ing.ingredient_food_id)!;
        acc.calories += Number(food.calories_per_100g) * ratio;
        acc.protein += Number(food.protein_per_100g) * ratio;
        acc.fat += Number(food.fat_per_100g) * ratio;
        acc.carbs += Number(food.carbs_per_100g) * ratio;
      } else {
        // Flat values for the given quantity (not per-100g scaled)
        acc.calories += Number(ing.calories_override ?? 0);
        acc.protein += Number(ing.protein_override ?? 0);
        acc.fat += Number(ing.fat_override ?? 0);
        acc.carbs += Number(ing.carbs_override ?? 0);
      }

      return acc;
    },
    { calories: 0, protein: 0, fat: 0, carbs: 0 },
  );
}
