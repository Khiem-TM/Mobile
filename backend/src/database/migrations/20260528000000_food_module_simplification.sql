-- Simplify food nutrition model and remove recipe/ingredient features.
-- Apply in staging/production before deploying entity changes if synchronize=false.

ALTER TABLE foods
  DROP COLUMN IF EXISTS sugar_per_100g,
  DROP COLUMN IF EXISTS sodium_per_100g,
  DROP COLUMN IF EXISTS cholesterol_per_100g;

ALTER TABLE meal_log_items
  DROP COLUMN IF EXISTS sugar_snapshot,
  DROP COLUMN IF EXISTS sodium_snapshot;

DROP TABLE IF EXISTS food_recipe_steps;
DROP TABLE IF EXISTS food_recipes;
DROP TABLE IF EXISTS food_ingredients;

CREATE INDEX IF NOT EXISTS idx_foods_public_active
  ON foods(is_custom, is_active, favorites_count);

CREATE INDEX IF NOT EXISTS idx_foods_custom_owner
  ON foods(created_by_user_id, is_custom, is_active);
