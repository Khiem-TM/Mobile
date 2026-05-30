-- Add a daily step goal to user health profiles.
-- Apply before deploying entity changes if synchronize=false.

ALTER TABLE user_health_profiles
  ADD COLUMN IF NOT EXISTS step_goal int DEFAULT 10000;

UPDATE user_health_profiles
SET step_goal = 10000
WHERE step_goal IS NULL;

