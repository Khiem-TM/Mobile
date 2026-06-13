-- Add a Cloudinary URL pointing to a .lottie animation for an exercise.
-- Apply before deploying entity changes if synchronize=false.

ALTER TABLE exercises
  ADD COLUMN IF NOT EXISTS lottie_asset varchar(500);
