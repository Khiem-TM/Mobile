-- Consolidate target weight fields on user health profiles.
-- Apply before deploying entity changes if synchronize=false.

UPDATE user_health_profiles
SET target_weight_kg = weight_goal_kg
WHERE target_weight_kg IS NULL
  AND weight_goal_kg IS NOT NULL;

ALTER TABLE user_health_profiles
  DROP COLUMN IF EXISTS weight_goal_kg;
