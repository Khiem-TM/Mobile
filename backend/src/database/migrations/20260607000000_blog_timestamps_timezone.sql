-- Blog timestamps were originally stored as timestamp without time zone.
-- Existing values represent UTC wall-clock time, so preserve that meaning
-- while converting them to timezone-aware instants.

ALTER TABLE blogs
  ALTER COLUMN created_at TYPE timestamptz
    USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN updated_at TYPE timestamptz
    USING updated_at AT TIME ZONE 'UTC',
  ALTER COLUMN deleted_at TYPE timestamptz
    USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE blog_comments
  ALTER COLUMN created_at TYPE timestamptz
    USING created_at AT TIME ZONE 'UTC',
  ALTER COLUMN updated_at TYPE timestamptz
    USING updated_at AT TIME ZONE 'UTC',
  ALTER COLUMN deleted_at TYPE timestamptz
    USING deleted_at AT TIME ZONE 'UTC';
