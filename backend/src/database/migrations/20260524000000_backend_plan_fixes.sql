-- VitalAI backend plan stability fixes.
-- Apply this in production/staging before deploying entity changes if synchronize=false.

ALTER TABLE meal_logs
  ALTER COLUMN log_date TYPE date USING log_date::date;

UPDATE meal_logs
SET log_date = CURRENT_DATE
WHERE log_date IS NULL;

ALTER TABLE meal_logs
  ALTER COLUMN meal_type TYPE text USING LOWER(meal_type::text);

UPDATE meal_logs
SET meal_type = 'breakfast'
WHERE meal_type IS NULL;

ALTER TABLE meal_logs
  ALTER COLUMN meal_type TYPE varchar(20)
  USING meal_type::varchar;

ALTER TABLE refresh_tokens
  ADD COLUMN IF NOT EXISTS token_sha256 varchar(64);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
  ON refresh_tokens(user_id);

ALTER TABLE activity_logs
  ADD COLUMN IF NOT EXISTS workout_calories_burned decimal(7,2) DEFAULT 0;

ALTER TABLE body_metrics
  ADD COLUMN IF NOT EXISTS measured_date date;

UPDATE body_metrics
SET measured_date = DATE(measured_at AT TIME ZONE 'UTC')
WHERE measured_at IS NOT NULL;

DELETE FROM body_metrics bm
USING body_metrics newer
WHERE bm.user_id = newer.user_id
  AND bm.measured_date = newer.measured_date
  AND (
    newer.measured_at > bm.measured_at
    OR (newer.measured_at = bm.measured_at AND newer.created_at > bm.created_at)
    OR (newer.measured_at = bm.measured_at AND newer.created_at = bm.created_at AND newer.id::text > bm.id::text)
  );

ALTER TABLE body_metrics
  ALTER COLUMN measured_date SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_body_metrics_user_measured_date
  ON body_metrics(user_id, measured_date);

DELETE FROM streaks
WHERE user_id IS NULL
   OR NOT EXISTS (
     SELECT 1 FROM users WHERE users.id = streaks.user_id
   );
