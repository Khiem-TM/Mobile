-- Admin Dashboard Production V1.
-- Apply before deploy when TypeORM synchronize=false.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notifications_type_enum') THEN
    ALTER TYPE notifications_type_enum ADD VALUE IF NOT EXISTS 'warning';
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS audit_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_user_id uuid NULL,
  actor_email varchar(255) NULL,
  action varchar(100) NOT NULL,
  target_type varchar(50) NOT NULL,
  target_id varchar(100) NULL,
  status varchar(20) NOT NULL DEFAULT 'success',
  ip_address varchar(100) NULL,
  user_agent text NULL,
  metadata jsonb NULL,
  error_message text NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE blogs
  ADD COLUMN IF NOT EXISTS deleted_at timestamp NULL;

ALTER TABLE blog_comments
  ADD COLUMN IF NOT EXISTS deleted_at timestamp NULL;

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_created
  ON audit_logs(actor_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_target
  ON audit_logs(target_type, target_id);

CREATE INDEX IF NOT EXISTS idx_users_admin_filters
  ON users(role, is_active, is_verified, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_foods_admin_filters
  ON foods(is_active, is_verified, is_custom, food_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_exercises_admin_filters
  ON exercises(is_active, exercise_type, category, muscle_group);

CREATE INDEX IF NOT EXISTS idx_blogs_admin_filters
  ON blogs(status, deleted_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_blog_comments_visible
  ON blog_comments(blog_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_meal_logs_log_date
  ON meal_logs(log_date);

CREATE INDEX IF NOT EXISTS idx_training_sessions_session_date
  ON training_sessions(session_date);

CREATE INDEX IF NOT EXISTS idx_ai_scan_logs_created_at
  ON ai_scan_logs(created_at);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_created_at
  ON chat_sessions(created_at);

CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at
  ON chat_messages(created_at);
