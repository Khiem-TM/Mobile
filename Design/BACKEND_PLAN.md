# VitalAI Backend — Audit & Implementation Plan

> Generated: 2026-05-24  
> Scope: Full backend audit + fix plan based on complete source reading  
> Working dir: `backend/src/`

---

## Phase 0 — Audit Summary (Completed)

All source files read. Issues are classified by severity and grouped into actionable phases below.

### Root-cause map

| Area | Files | Critical Issues |
|---|---|---|
| Auth / User | `auth.service.ts`, `users.service.ts` | Email-verify gate blocks login; logout-all leaks Redis tokens; duplicate calorie columns |
| Onboarding | `users.service.ts`, `tdee.util.ts`, `body-metrics.service.ts` | TDEE auto-calc skipped for new users (no body metric yet); no onboarding endpoint |
| Food | `foods.controller.ts`, `meal-logs.service.ts` | Route ordering bug shadows `/foods/custom`; `log_date` type mismatch; no cache invalidation |
| Train | `training.service.ts`, `activity-logs.service.ts` | N+1 per-exercise query; workout sync clobbers manual calorie entries |
| Notifications | `notifications.service.ts`, `activity-logs.service.ts` | `goal_progress` / `reminder` types never triggered; water-goal notification spams |
| Module wiring | `train.module.ts`, `jwt.strategy.ts` | `BODY_PHOTOS_REPOSITORY` → wrong class; JWT `jti` not enforced |
| Redis | `redis.service.ts` | `KEYS` command (blocks loop); default port mismatch 6380 vs 6379 |
| Schema | `meal-log.entity.ts`, `user-health-profile.entity.ts` | `log_date` is `timestamptz` not `date`; two overlapping calorie columns |

---

## Phase 1 — Critical Bug Fixes (Runtime-Breaking)

These will cause data corruption or runtime failures and must be fixed first.

### 1.1 Fix `BODY_PHOTOS_REPOSITORY` wrong class binding

**File:** `backend/src/modules/train/train.module.ts`

**Problem:** `{ provide: BODY_PHOTOS_REPOSITORY, useClass: BodyMetricsRepository }` — body-progress-photos injection token receives the wrong repository class.

**Fix:** Create `BodyProgressPhotosRepository` (if missing) or wire correctly:
```typescript
// train.module.ts — change:
{ provide: BODY_PHOTOS_REPOSITORY, useClass: BodyProgressPhotosRepository }
```
Also create `body-progress-photos.repository.ts` and `body-progress-photos.repository.interface.ts` if they don't exist, following the same pattern as `body-metrics.repository.ts`.

**Verification:** `grep -r 'BODY_PHOTOS_REPOSITORY' src/` — all injection sites must resolve without runtime error.

---

### 1.2 Fix route ordering: `GET /foods/custom` shadowed by `GET /foods/:id`

**File:** `backend/src/modules/food/controllers/foods.controller.ts`

**Problem:** `@Get('custom')` is declared after `@Get(':id')`. NestJS matches routes in declaration order — `GET /foods/custom` is captured by `:id = 'custom'` and returns 404.

**Fix:** Move the `@Get('custom')` handler **above** `@Get(':id')` in the file.

**Anti-pattern guard:** Never place a static-path handler after a wildcard `:param` handler in the same controller.

**Verification:** `GET /foods/custom` returns the custom foods list, not a 404.

---

### 1.3 Fix `log_date` column type mismatch in MealLog

**File:** `backend/src/modules/food/entities/meal-log.entity.ts`

**Problem:** `log_date` is declared `timestamptz` but service code always uses date strings (`'2024-01-15'`). PostgreSQL date-grouping breaks across timezones when a time component is present.

**Fix:**
```typescript
// meal-log.entity.ts — change column type:
@Column({ type: 'date', name: 'log_date' })
log_date: string;
```

Also write a migration:
```sql
ALTER TABLE meal_logs ALTER COLUMN log_date TYPE date USING log_date::date;
```

**Verification:** `SELECT DISTINCT log_date FROM meal_logs` returns clean date strings; daily summary groups correctly.

---

### 1.4 Fix logout-all: Redis tokens cannot be derived

**File:** `backend/src/modules/user/services/auth.service.ts`  
**File:** `backend/src/modules/user/entities/refresh-token.entity.ts`

**Problem:** Logout-all deletes DB `RefreshToken` rows but cannot revoke Redis entries because only bcrypt hash (not SHA-256 hash) is stored in DB. Old tokens remain valid in Redis for up to 30 days.

**Fix — two-step:**

Step A — Store the SHA-256 hash in `refresh_tokens` table alongside `token_hash` (bcrypt):
```typescript
// refresh-token.entity.ts — add column:
@Column({ name: 'token_sha256', length: 64, nullable: true })
token_sha256: string;   // SHA-256 of raw token — used to derive the Redis key
```

Step B — In `logout()` logout-all path, load all `token_sha256` values for the user, delete the corresponding Redis keys:
```typescript
// auth.service.ts — logout-all block:
const tokens = await this.refreshTokenRepository.find({ where: { user: { id: userId } } });
await Promise.all(tokens.map(t => this.redisService.del(`rt:${t.token_sha256}`)));
await this.refreshTokenRepository.delete({ user: { id: userId } });
```

Also fix `revoked_at` field: instead of hard-deleting tokens on single-device logout, set `revoked_at = new Date()` so the audit trail is preserved.

**Migration:**
```sql
ALTER TABLE refresh_tokens ADD COLUMN token_sha256 varchar(64);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

**Verification:** After logout-all, any previous access token is rejected; Redis has no remaining `rt:*` keys for that user.

---

### 1.5 Fix JWT: ensure all tokens include `jti` claim

**File:** `backend/src/modules/user/services/auth.service.ts`

**Problem:** `JwtStrategy` silently skips the blacklist check when `jti` is absent. If any code path issues tokens without `jti`, those tokens can never be revoked.

**Fix:** In `generateAuthResponse`, always include `jti`:
```typescript
import { v4 as uuidv4 } from 'uuid';
// access token payload:
const jti = uuidv4();
const payload = { sub: user.id, email: user.email, role: user.role, jti };
```

**Verification:** Decode any issued JWT with `jwt.io` — `jti` field is always present.

---

## Phase 2 — Feature: Remove Email Verification → Welcome Email Only

### 2.1 Add `sendWelcomeEmail` to MailerService

**File:** `backend/src/modules/support/mailer/mailer.service.ts`

Add a `sendWelcomeEmail(email: string, displayName: string): Promise<void>` method with an HTML welcome template (Vietnamese, matching existing email style). No token or link needed.

```typescript
async sendWelcomeEmail(email: string, displayName: string): Promise<void> {
  if (!this.transporter) {
    this.logger.warn(`[DEV] Welcome email would be sent to ${email}`);
    return;
  }
  const from = this.configService.get('MAIL_FROM', 'VitalAI <noreply@vitalai.app>');
  await this.transporter.sendMail({
    from, to: email,
    subject: 'Chào mừng bạn đến với VitalAI!',
    html: `<div ...>Xin chào ${displayName}, chào mừng bạn!</div>`,
  });
}
```

---

### 2.2 Update `register()` — skip verification, send welcome email

**File:** `backend/src/modules/user/services/auth.service.ts`

**Changes:**
1. Set `is_verified: true` at creation time (remove `false` default).
2. Replace `this.sendEmailVerification(user.email).catch(...)` with `this.mailerService.sendWelcomeEmail(user.email, user.display_name).catch(...)`.
3. Remove `is_verified` check from `login()` (lines 152–154).

```typescript
// register() — change:
const user = this.userRepository.create({
  email: email.toLowerCase().trim(),   // normalize email
  password_hash,
  display_name,
  is_verified: true,   // verified immediately
});
await this.userRepository.save(user);

this.mailerService.sendWelcomeEmail(user.email, user.display_name).catch(err =>
  this.logger.error('Failed to send welcome email:', err),
);
```

---

### 2.3 Remove email verification endpoints from controller

**File:** `backend/src/modules/user/controllers/auth.controller.ts`

Remove or mark `@deprecated`:
- `POST /auth/send-verification`
- `POST /auth/verify-email`

Keep the `EmailVerification` entity and table (it already handles nothing after this change — safe to remove in a later cleanup migration if desired).

**Note:** Google/Facebook OAuth users continue to register with `is_verified: true` (no change needed there).

**Verification:** `POST /auth/register` → user can immediately `POST /auth/login` without verifying email. Welcome email is logged in dev mode.

---

## Phase 3 — Feature: Onboarding Calorie Calculation & Goal Setup

### Problem Summary

Current `updateHealthProfile` auto-calculates macros only when `latestMetric?.tdee` exists. For a brand-new user, no `BodyMetric` record exists → TDEE is null → macro goals are never set after onboarding.

### 3.1 Add inline TDEE calculation fallback in `updateHealthProfile`

**File:** `backend/src/modules/user/services/users.service.ts`

When `goalType` is provided but `latestMetric?.tdee` is null, calculate TDEE directly from the merged profile data using `TDEEUtil`:

```typescript
import { TDEEUtil } from '../../../common/utils/tdee.util';

// In updateHealthProfile(), replace the existing macro-calc block:
if (data.goalType && !data.proteinGoalG && !data.fatGoalG && !data.carbsGoalG) {
  const latestMetric = await this.bodyMetricsService.getLatest(userId);
  
  let tdee: number | null = latestMetric?.tdee ? Number(latestMetric.tdee) : null;

  // Fallback: calculate TDEE inline from health profile data
  if (!tdee) {
    const p = { ...profile, ...data } as any;
    if (p.initialWeightKg && p.heightCm && p.birthDate && p.gender && p.activityLevel) {
      const age = Math.floor(
        (Date.now() - new Date(p.birthDate).getTime()) / (365.25 * 24 * 3600 * 1000)
      );
      const bmr = TDEEUtil.calculateBMR(
        Number(p.initialWeightKg), Number(p.heightCm), age, p.gender
      );
      tdee = TDEEUtil.calculateTDEE(bmr, p.activityLevel);
    }
  }

  if (tdee) {
    let calories = tdee;
    if (data.goalType === 'lose_weight') calories = tdee - 500;
    else if (data.goalType === 'gain_weight') calories = tdee + 300;
    else if (data.goalType === 'gain_muscle') calories = tdee + 300;
    else if (data.goalType === 'bulking') calories = tdee + 500;
    else if (data.goalType === 'cutting') calories = tdee - 500;
    // maintain / improve_endurance: use tdee as-is

    merged.dailyCaloriesGoal = Number(calories.toFixed(2));
    merged.caloriesGoal = merged.dailyCaloriesGoal;    // sync both columns until migration
    merged.proteinGoalG = Number(((calories * 0.3) / 4).toFixed(2));
    merged.fatGoalG = Number(((calories * 0.3) / 9).toFixed(2));
    merged.carbsGoalG = Number(((calories * 0.4) / 4).toFixed(2));

    // Auto-set water goal: 35ml per kg of body weight, minimum 2000ml
    if (!data.waterGoalMl && merged.initialWeightKg) {
      merged.waterGoalMl = Math.max(2000, Math.round(Number(merged.initialWeightKg) * 35));
    }
  }
}
```

---

### 3.2 Add dedicated `POST /users/me/onboarding` endpoint

**Files:**  
- `backend/src/modules/user/controllers/users.controller.ts` — add route  
- `backend/src/modules/user/services/users.service.ts` — add `completeOnboarding()`  
- `backend/src/modules/user/dto/onboarding.dto.ts` — new DTO

This endpoint accepts all onboarding data at once and:
1. Saves the health profile (triggers inline TDEE calc per 3.1)
2. Creates an initial `BodyMetric` record (weight = `initialWeightKg`) — this auto-calculates and persists BMI/BMR/TDEE via `BodyMetricsService.upsert`
3. Creates a `SYSTEM` notification: "Mục tiêu của bạn đã được thiết lập!" with the calculated daily calories
4. Returns the saved health profile with `dailyCaloriesGoal`, `proteinGoalG`, `fatGoalG`, `carbsGoalG`, `waterGoalMl`

**DTO (`onboarding.dto.ts`):**
```typescript
export class OnboardingDto {
  @IsDateString()           birthDate: string;
  @IsEnum(['male','female','other']) gender: string;
  @IsNumber() @Min(50) @Max(300)   heightCm: number;
  @IsNumber() @Min(20) @Max(500)   initialWeightKg: number;
  @IsEnum(ActivityLevel)            activityLevel: string;
  @IsEnum(GoalType)                 goalType: string;
  @IsNumber() @IsOptional()         targetWeightKg?: number;
  @IsEnum(DietType) @IsOptional()   dietType?: string;
  @IsArray() @IsOptional()          foodAllergies?: string[];
}
```

**Service method:**
```typescript
async completeOnboarding(userId: string, dto: OnboardingDto): Promise<UserHealthProfile> {
  // 1. Save health profile (with inline TDEE calc)
  const profile = await this.updateHealthProfile(userId, {
    birthDate: dto.birthDate,
    gender: dto.gender,
    heightCm: dto.heightCm,
    initialWeightKg: dto.initialWeightKg,
    activityLevel: dto.activityLevel,
    goalType: dto.goalType,
    targetWeightKg: dto.targetWeightKg,
    dietType: dto.dietType,
    foodAllergies: dto.foodAllergies ?? [],
  });

  // 2. Create initial body metric (triggers BodyMetricsService BMI/BMR/TDEE auto-calc)
  await this.bodyMetricsService.upsert(userId, {
    weightKg: dto.initialWeightKg,
    notes: 'Initial weight from onboarding',
  });

  // 3. Welcome notification with calorie goal
  if (profile.dailyCaloriesGoal) {
    await this.notificationsService.create(
      userId,
      NotificationType.SYSTEM,
      'Mục tiêu đã được thiết lập! 🎯',
      `Mục tiêu calo hàng ngày của bạn: ${Math.round(Number(profile.dailyCaloriesGoal))} kcal`,
    );
  }

  return profile;
}
```

**Note:** `UsersService` needs `NotificationsService` injected (add to constructor + module).

**Verification checklist:**
- `POST /users/me/onboarding` with full data → response contains `dailyCaloriesGoal`, `proteinGoalG`, `fatGoalG`, `carbsGoalG`, `waterGoalMl`
- `GET /train/body-metrics` → initial body metric exists with `bmi`, `bmr`, `tdee` fields populated
- `GET /notifications` → one SYSTEM notification about calorie goal
- Repeat call → updates existing health profile (upsert), does not duplicate body metric

---

## Phase 4 — Schema & Data Consistency Fixes

### 4.1 Remove duplicate calorie column

**Problem:** `user_health_profiles` has both `caloriesGoal` and `dailyCaloriesGoal`. Phase 3 syncs both as a bridge.

**Final fix (migration):**
```sql
-- After deploying Phase 3:
UPDATE user_health_profiles SET calories_goal = daily_calories_goal WHERE daily_calories_goal IS NOT NULL;
ALTER TABLE user_health_profiles DROP COLUMN daily_calories_goal;
```
Then remove `dailyCaloriesGoal` from the entity and all references.

**Verification:** `SELECT column_name FROM information_schema.columns WHERE table_name='user_health_profiles'` — only `calories_goal` present.

---

### 4.2 Fix MealType enum to lowercase

**Problem:** `MealType` stores `'BREAKFAST'`, `'LUNCH'`, `'DINNER'`, `'SNACK'` in uppercase. All other enums use lowercase. This creates inconsistent DB queries.

**File:** `backend/src/common/enums/meal-type.enum.ts`
```typescript
export enum MealType {
  BREAKFAST = 'breakfast',
  LUNCH     = 'lunch',
  DINNER    = 'dinner',
  SNACK     = 'snack',
}
```

**Migration:**
```sql
UPDATE meal_logs SET meal_type = LOWER(meal_type);
-- Then alter enum type in PostgreSQL accordingly
```

**Verification:** All meal logs have lowercase `meal_type` values.

---

### 4.3 Fix DTO validation for `activityLevel` and `goalType`

**File:** `backend/src/modules/user/dto/update-health-profile.dto.ts`

Replace `@IsString()` + `@Length()` with `@IsEnum()`:
```typescript
import { GoalType } from '../../../common/enums/goal-type.enum';
import { ActivityLevel } from '../../../common/enums/activity-level.enum';  // create if missing

@IsEnum(ActivityLevel) @IsOptional() activityLevel?: string;
@IsEnum(GoalType)      @IsOptional() goalType?: string;
```

Also create `activity-level.enum.ts`:
```typescript
export enum ActivityLevel {
  SEDENTARY         = 'sedentary',
  LIGHTLY_ACTIVE    = 'lightly_active',
  MODERATELY_ACTIVE = 'moderately_active',
  VERY_ACTIVE       = 'very_active',
  EXTRA_ACTIVE      = 'extra_active',
}
```

---

### 4.4 Add email normalization in RegisterDto

**File:** `backend/src/modules/user/dto/register.dto.ts`
```typescript
import { Transform } from 'class-transformer';

@Transform(({ value }) => value?.toLowerCase().trim())
@IsEmail()
email: string;
```

---

### 4.5 Add ORM FK relation on Streak entity

**File:** `backend/src/modules/user/entities/streak.entity.ts`

Add proper `@ManyToOne` so TypeORM enforces the FK and cascade deletes:
```typescript
@ManyToOne(() => User, { onDelete: 'CASCADE' })
@JoinColumn({ name: 'user_id' })
user: User;
```

---

### 4.6 Fix `BodyMetric.photos` relation typing

**File:** `backend/src/modules/train/entities/body-metric.entity.ts`

Replace string-literal lazy reference with proper typed relation:
```typescript
import { BodyProgressPhoto } from './body-progress-photo.entity';

@OneToMany(() => BodyProgressPhoto, (photo) => photo.bodyMetric)
photos: BodyProgressPhoto[];
```

---

## Phase 5 — Performance Improvements

### 5.1 Fix N+1 in `createWorkoutSession`

**File:** `backend/src/modules/train/services/training.service.ts`

**Problem:** `_calcCalories` issues one `findById` per exercise inside a loop.

**Fix:** Batch-load all exercises before the loop:
```typescript
const exerciseIds = dto.details.map(d => d.exerciseId);
const exercises = await this.exerciseRepository.findByIds(exerciseIds);
const exerciseMap = new Map(exercises.map(e => [e.id, e]));
// then inside loop: const exercise = exerciseMap.get(detail.exerciseId);
```

---

### 5.2 Fix streak nightly reset N+1

**File:** `backend/src/modules/user/services/streaks.service.ts`

Replace per-row `updateStreak` calls with a single bulk UPDATE:
```typescript
// Replace the loop with:
await this.streakRepository
  .createQueryBuilder()
  .update(Streak)
  .set({ current_streak: 0 })
  .where('current_streak > 0')
  .andWhere('last_activity_date < :yesterday', { yesterday })
  .execute();
```

---

### 5.3 Fix `delByPattern` to use SCAN instead of KEYS

**File:** `backend/src/modules/support/redis/redis.service.ts`

`KEYS` blocks the Redis event loop on large keyspaces.

```typescript
async delByPattern(pattern: string): Promise<void> {
  let cursor = '0';
  do {
    const [nextCursor, keys] = await this.client.scan(cursor, 'MATCH', pattern, 'COUNT', 100);
    cursor = nextCursor;
    if (keys.length > 0) await this.client.del(...keys);
  } while (cursor !== '0');
}
```

---

### 5.4 Add Redis cache invalidation after food mutations

**File:** `backend/src/modules/food/services/foods.service.ts`

After `createCustom`, `setIngredients`, `upsertRecipe`, `uploadImage`, `removeImage`:
```typescript
await this.redisService.del(`cache:foods:one:${foodId}`);
// also clear list caches:
await this.redisService.delByPattern('cache:foods:list:*');
```

---

### 5.5 Fix weekly/monthly report in-memory filtering

**File:** `backend/src/modules/user/services/dashboard.service.ts`

Pass `fromDate`/`toDate` directly to `getWorkoutHistoryRange` instead of fetching by limit and filtering in memory:
```typescript
// getWeeklyReport — replace:
const sessions = await this.trainingService.getWorkoutHistoryRange(userId, weekStart, weekEnd);
// getMonthlyReport:
const sessions = await this.trainingService.getWorkoutHistoryRange(userId, monthStart, monthEnd);
```

---

### 5.6 Fix Redis default port mismatch

**File:** `backend/src/modules/support/redis/redis.service.ts`

Change hardcoded default from `6380` to `6379`:
```typescript
const port = this.configService.get<number>('REDIS_PORT', 6379);
```

---

## Phase 6 — Business Logic Fixes

### 6.1 Fix water goal notification spam

**File:** `backend/src/modules/train/services/activity-logs.service.ts`

Replace `notificationsService.create(...)` with `notificationsService.createOncePerDay(...)` when the water goal is reached:
```typescript
await this.notificationsService.createOncePerDay(
  userId, NotificationType.SYSTEM,
  'Mục tiêu nước hôm nay! 💧',
  `Bạn đã uống đủ ${waterGoalMl}ml nước hôm nay. Tuyệt vời!`,
);
```

---

### 6.2 Fix `caloriesBurned` merge strategy

**Problem:** `activity_logs.calories_burned` is shared by both manual entries and workout-sync. Workout sync overwrites manual entries.

**Fix — add a separate column:**

Migration:
```sql
ALTER TABLE activity_logs ADD COLUMN workout_calories_burned decimal(7,2) DEFAULT 0;
```

Entity change (`activity-log.entity.ts`):
```typescript
@Column({ type: 'decimal', precision: 7, scale: 2, default: 0, name: 'workout_calories_burned' })
workoutCaloriesBurned: number;
```

Update `setWorkoutCalories` to write `workoutCaloriesBurned`, not `caloriesBurned`.

In `getByDate` response, compute `totalCaloriesBurned = caloriesBurned + workoutCaloriesBurned`.

---

### 6.3 Fix `updateWorkoutSession` to support date change

**File:** `backend/src/modules/train/dto/update-training.dto.ts`

Add `sessionDate?: string` to `UpdateWorkoutSessionDto`.

**File:** `backend/src/modules/train/services/training.service.ts`

In `updateWorkoutSession`:
```typescript
if (dto.sessionDate && dto.sessionDate !== session.sessionDate) {
  // Zero out old date's workout calories
  await this._syncActivityLog(userId, session.sessionDate, 0);
  session.sessionDate = dto.sessionDate;
}
```

---

### 6.4 Include sugar/sodium in daily meal summary

**File:** `backend/src/modules/food/services/meal-logs.service.ts`

In `_summarizeLogs`, add to the accumulator:
```typescript
total_sugar  += Number(item.sugar_snapshot  ?? 0);
total_sodium += Number(item.sodium_snapshot ?? 0);
```

Return them in the summary object.

---

### 6.5 Standardize error messages to English

**File:** `backend/src/modules/user/services/auth.service.ts`

Replace Vietnamese error message at ~line 139:
```typescript
// Change:
throw new UnauthorizedException('Tài khoản này đăng nhập bằng Google...');
// To:
throw new UnauthorizedException('This account uses Google login. Please use Google Sign-In.');
```

---

### 6.6 Fix Google OAuth callback token exposure

**File:** `backend/src/modules/user/controllers/auth.controller.ts`

Instead of placing tokens in the URL, use a short-lived one-time code stored in Redis:
```typescript
// In googleCallback:
const otp = uuidv4();
await this.redisService.set(`oauth_code:${otp}`, JSON.stringify(authResponse), 300); // 5min TTL
res.redirect(`${frontendUrl}/oauth-callback?code=${otp}`);
```

Add `GET /auth/oauth-token?code=:code` endpoint that exchanges the OTP for real tokens (one-time use, deletes from Redis after read using `getdel`).

---

## Phase 7 — Notification System Completeness

### 7.1 Trigger `goal_progress` notifications

**Where to hook:** `meal-logs.service.ts` in `getDailySummary` (or after every item add/update).

Logic:
- After a meal item is added, if `total_calories_today >= dailyCaloriesGoal * 0.9` AND `< dailyCaloriesGoal * 1.1`, trigger goal notification once per day.
- Use `notificationsService.createOncePerDay` so it only fires once.

```typescript
// In addItemToLog(), after the item is saved:
const summary = await this.getDailySummary(userId, date);
if (profile?.dailyCaloriesGoal) {
  const goal = Number(profile.dailyCaloriesGoal);
  const pct  = summary.total_calories / goal;
  if (pct >= 0.9 && pct <= 1.1) {
    await this.notificationsService.createOncePerDay(
      userId, NotificationType.GOAL_PROGRESS,
      'Đạt mục tiêu calo hôm nay! 🎉',
      `Bạn đã nạp ${Math.round(summary.total_calories)} / ${Math.round(goal)} kcal`,
    );
    // Also trigger calorie-goal streak
    await this.streaksService.updateActivity(userId, StreakType.CALORIE_GOAL, date);
  }
}
```

---

### 7.2 Trigger `workout` streak on session creation

**File:** `backend/src/modules/train/services/training.service.ts`

Already present but verify it is called in `createWorkoutSession`:
```typescript
await this.streaksService.updateActivity(userId, StreakType.WORKOUT, session.sessionDate);
```

---

### 7.3 Add daily reminder cron

**File:** `backend/src/modules/user/services/streaks.scheduler.ts` (or new `notifications.scheduler.ts`)

Add a cron that runs at 8pm: for users who have not logged any meal today, send a `REMINDER` notification:
```typescript
@Cron('0 20 * * *')  // 8pm server time
async sendDailyReminders(): Promise<void> {
  // Find users with no meal log today
  // Use batch createMany to avoid N individual inserts
  // Limit to users who have been active in the past 7 days (not spam to inactive users)
}
```

**Note:** This is best suited for Kafka (Phase 8). If implemented synchronously, use batch `createMany`.

---

## Phase 8 (Optional) — Apache Kafka for Async Processing

### 8.1 Infrastructure setup

**Install:**
```bash
npm install @nestjs/microservices kafkajs
```

**Docker Compose addition** (for local dev):
```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.4.0
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181

kafka:
  image: confluentinc/cp-kafka:7.4.0
  depends_on: [zookeeper]
  ports: ["9092:9092"]
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

---

### 8.2 Topics and event definitions

Create `backend/src/common/events/` directory:

| Topic | Events | Producer | Consumer |
|---|---|---|---|
| `vitalai.user.events` | `user.registered`, `user.onboarding.completed`, `user.profile.updated` | `AuthService`, `UsersService` | `MailerConsumer`, `NotificationsConsumer`, `RagConsumer` |
| `vitalai.activity.events` | `meal.logged`, `water.logged`, `workout.completed`, `steps.logged` | `MealLogsService`, `ActivityLogsService`, `TrainingService` | `StreaksConsumer`, `NotificationsConsumer`, `DashboardInvalidateConsumer` |
| `vitalai.notification.events` | `notification.create`, `notification.bulk` | All services | `NotificationsConsumer` (persists to DB) |

---

### 8.3 Events module

**File:** `backend/src/modules/events/events.module.ts`

```typescript
@Module({
  imports: [
    ClientsModule.register([{
      name: 'KAFKA_CLIENT',
      transport: Transport.KAFKA,
      options: {
        client: { brokers: [process.env.KAFKA_BROKER ?? 'localhost:9092'] },
        consumer: { groupId: 'vitalai-backend' },
      },
    }]),
  ],
  providers: [EventsProducerService],
  exports: [EventsProducerService],
})
export class EventsModule {}
```

---

### 8.4 Replace fire-and-forget calls with Kafka events

**Candidates for migration to async:**

| Current synchronous call | Replace with event |
|---|---|
| `mailerService.sendWelcomeEmail(...)` in `register()` | Emit `user.registered` → `MailerConsumer` handles |
| `ragEmbedService.triggerUserEmbed(...)` in health profile update | Emit `user.profile.updated` → `RagConsumer` handles |
| `ragEmbedService.triggerUserEmbed(...)` in body metrics upsert | Emit `user.profile.updated` |
| `streaksService.updateActivity(...)` in login | Emit `user.activity` → `StreaksConsumer` handles |
| `notificationsService.create(...)` in streaks service | Emit `notification.create` → `NotificationsConsumer` handles |
| Water goal notification in `logWater` | Emit `activity.water.logged` → `NotificationsConsumer` checks goal |
| Dashboard cache invalidation after meal log | Emit `meal.logged` → `DashboardInvalidateConsumer` |

---

### 8.5 Verification checklist for Kafka

- [ ] `docker-compose up kafka zookeeper` — Kafka starts without error
- [ ] `POST /auth/register` → welcome email is delivered asynchronously (consumer logs show processing)
- [ ] `POST /users/me/onboarding` → RAG embed is triggered via Kafka, not HTTP inline call
- [ ] `POST /train/sessions` → streak updated via consumer (check `GET /streaks` after)
- [ ] Consumer group `vitalai-backend` visible in Kafka UI with active offsets
- [ ] Graceful shutdown: `app.close()` flushes pending messages

---

## Implementation Order (recommended)

```
Phase 1  → Phase 2  → Phase 3  → Phase 4  → Phase 5  → Phase 6  → Phase 7  → Phase 8
Critical    No-verify  Onboard    Schema     Perf       Logic      Notifs     Kafka
  bugs       email      flow       fixes     fixes      fixes     complete   (optional)
~1 day     ~2 hrs     ~3 hrs     ~2 hrs     ~2 hrs     ~3 hrs     ~2 hrs     ~2 days
```

---

## Anti-Pattern Guards (for all phases)

- Do NOT use `@InjectRepository(Entity)` in services that belong to a different module — go through the repository interface or service boundary.
- Do NOT call `KEYS` in Redis for production use — always use `SCAN`.
- Do NOT place static route handlers (`@Get('custom')`) after wildcard param handlers (`@Get(':id')`) in NestJS controllers.
- Do NOT store raw tokens or SHA-256 hashes in URLs or logs — only bcrypt/SHA-256 hashes in DB, SHA-256 keys in Redis.
- Do NOT call `TDEEUtil` without validating that `weightKg`, `heightCm`, `age`, and `activityLevel` are all present and non-zero.
- Do NOT share the `caloriesBurned` column between manual input and workout-derived values — keep them separate.
