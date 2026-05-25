-- ============================================================
-- Train Module Migration — Run BEFORE starting the backend
-- Rename tables + columns to match new entity structure.
-- TypeORM synchronize:true handles adding new columns after this.
-- ============================================================

-- 1. Rename workout_sessions → training_sessions
ALTER TABLE workout_sessions RENAME TO training_sessions;
ALTER TABLE training_sessions RENAME COLUMN session_name TO title;
ALTER TABLE training_sessions RENAME COLUMN notes TO note;

-- 2. Rename workout_session_details → training_session_items
ALTER TABLE workout_session_details RENAME TO training_session_items;
ALTER TABLE training_session_items RENAME COLUMN workout_session_id TO session_id;
ALTER TABLE training_session_items RENAME COLUMN reps_per_set TO reps;
ALTER TABLE training_session_items RENAME COLUMN notes TO note;

-- Update FK constraint name (PostgreSQL auto-names these; find and rename)
-- Safer: just drop & re-add (TypeORM synchronize will recreate on next start)
DO $$
DECLARE
  fk_name TEXT;
BEGIN
  SELECT tc.constraint_name INTO fk_name
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
  WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_name = 'training_session_items'
    AND kcu.column_name = 'session_id';

  IF fk_name IS NOT NULL THEN
    EXECUTE 'ALTER TABLE training_session_items DROP CONSTRAINT ' || quote_ident(fk_name);
  END IF;
END $$;

ALTER TABLE training_session_items
  ADD CONSTRAINT fk_tsi_session
  FOREIGN KEY (session_id) REFERENCES training_sessions(id) ON DELETE CASCADE;

-- 3. Rename exercise_user_favorites → favorite_exercises
ALTER TABLE exercise_user_favorites RENAME TO favorite_exercises;

-- Update FK constraint for favorite_exercises.user_id (already cascade — just rename table)
-- TypeORM will handle constraint names on next sync.

-- 4. Update exercises table — rename columns
ALTER TABLE exercises RENAME COLUMN primary_muscle_group TO muscle_group;
ALTER TABLE exercises DROP COLUMN IF EXISTS intensity;

-- New columns on exercises (TypeORM sync adds these, but explicit here for clarity)
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS exercise_type    VARCHAR(10)   NOT NULL DEFAULT 'SPORT';
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS category         VARCHAR(50);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS difficulty_level VARCHAR(20)   NOT NULL DEFAULT 'BEGINNER';
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS default_sets              INT;
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS default_reps              INT;
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS default_weight_kg         DECIMAL(6,2);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS target_muscle_group       VARCHAR(50);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS rest_time_seconds         INT;
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS default_duration_minutes  INT;
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS default_intensity_level   VARCHAR(10);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS movement_type             VARCHAR(50);
ALTER TABLE exercises ADD COLUMN IF NOT EXISTS estimated_calories_per_minute DECIMAL(5,2);

-- 5. Update body_metrics table — rename columns
ALTER TABLE body_metrics RENAME COLUMN recorded_at     TO measured_at;
ALTER TABLE body_metrics RENAME COLUMN body_fat_pct    TO body_fat_percent;

-- New columns on body_metrics
ALTER TABLE body_metrics ADD COLUMN IF NOT EXISTS height_cm DECIMAL(5,1);
ALTER TABLE body_metrics ADD COLUMN IF NOT EXISTS arm_cm    DECIMAL(5,1);

-- 6. Update activity_logs table — remove old columns, add new ones
ALTER TABLE activity_logs DROP COLUMN IF EXISTS calories_burned;
ALTER TABLE activity_logs DROP COLUMN IF EXISTS workout_calories_burned;
ALTER TABLE activity_logs DROP COLUMN IF EXISTS active_minutes;
ALTER TABLE activity_logs DROP COLUMN IF EXISTS exercise_notes;

ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS note        TEXT;
ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS sleep_hours DECIMAL(4,2);
ALTER TABLE activity_logs ADD COLUMN IF NOT EXISTS mood        VARCHAR(20);

-- 7. Add new columns on training_session_items
ALTER TABLE training_session_items ADD COLUMN IF NOT EXISTS exercise_type    VARCHAR(10) NOT NULL DEFAULT 'SPORT';
ALTER TABLE training_session_items ADD COLUMN IF NOT EXISTS intensity_level  VARCHAR(10);
ALTER TABLE training_session_items ADD COLUMN IF NOT EXISTS distance_km      DECIMAL(7,3);
ALTER TABLE training_session_items ADD COLUMN IF NOT EXISTS pace             VARCHAR(20);
ALTER TABLE training_session_items ADD COLUMN IF NOT EXISTS rest_time_seconds INT;

-- ============================================================
-- Done. Start the backend — TypeORM synchronize will add any
-- remaining columns and indexes automatically.
-- ============================================================
