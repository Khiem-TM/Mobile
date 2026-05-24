# VitalAI Mobile — Backend/Mobile Consistency & Completion Plan

**Generated:** 2026-05-24  
**Scope:** Android Kotlin (Jetpack Compose, MVVM, Retrofit, Hilt)  
**Backend ref:** NestJS at `backend/` — 114 endpoints across 15 modules  
**Mobile ref:** Android at `mobile/` — 44 API calls, 25 screens, 10 repositories

---

## Phase 0: Audit Summary

### Critical Bugs (Silent failures today)

| # | Module | Mobile (Wrong) | Backend (Correct) | Impact |
|---|--------|---------------|-------------------|--------|
| B1 | Dashboard | `POST dashboard/water` `{"amount": ml}` | `PATCH activity-logs/water` `{logDate, waterMl}` | Quick-add water BROKEN (404) |
| B2 | Dashboard | `POST dashboard/steps` `{"amount": steps}` | `PATCH activity-logs/steps` `{logDate, steps}` | Quick-add steps BROKEN (404) |
| B3 | Body Metrics | `addMetric(BodyMetricDto)` — sends `id`, `date`, `muscleMassKg`, `bmi` | `POST body-metrics` expects `{recordedAt, weightKg, bodyFatPct, waistCm…}` | Weight logging BROKEN |
| B4 | Body Metrics | Period params: `1W`, `1M`, `3M` | Backend expects: `week`, `month`, `3months`, `6months`, `year` | Metrics chart BROKEN (404) |
| B5 | Auth | `logout()` only clears local tokens | `POST /auth/logout` revokes server-side JTI | Security: token stays live after logout |
| B6 | Auth | `ForgotPasswordScreen` — no ViewModel, no API call | `POST /auth/forgot-password` `{email}` exists | Password reset non-functional |

### Feature Gaps (Mobile missing, Backend ready)

**Priority 1 — Core User Flows:**
- Edit logged food item quantity (`PATCH /meal-logs/:id/items/:itemId`)
- Delete full meal log (`DELETE /meal-logs/:id`)
- View meal history by date range (`GET /meal-logs/history`)
- Rename / reschedule workout session (`PATCH /training/sessions/:id`)
- Remove exercise from session (`DELETE /training/sessions/:id/exercises/:detailId`)
- Log calories burned manually (`PATCH /activity-logs/calories-burned`)
- Weekly dashboard view (`GET /dashboard/weekly`)
- Monthly dashboard view (`GET /dashboard/monthly`)

**Priority 2 — Progress & Social:**
- Body measurements input (waist, hip, chest, neck cm) — UpsertBodyMetricDto
- Weight progress summary card (`GET /body-metrics/summary`)
- Progress photo upload/delete (`POST/DELETE /body-metrics/photos`)
- Blog like toggle (`POST /blogs/:id/like`, `GET /blogs/:id/liked`)
- Blog comments (`GET/POST /blogs/:id/comments`)
- Dynamic tag list from backend (`GET /blogs/tags`)

**Priority 3 — Content & Discovery:**
- Food/dish explore section (`GET /foods/explore`)
- Recipe detail view (`GET /foods/:id/recipe`)
- Custom food list (`GET /foods/custom`)
- AI food scan via camera (`POST /ai-scan/analyze`)

**Priority 4 — Auth Completeness:**
- Full forgot/reset password flow (`POST /auth/reset-password`)
- Delete notification (`DELETE /notifications/:id`)

---

## Phase 1: Critical Bug Fixes
**Goal:** Fix 6 silent failures so core features work.

### Task 1.1 — Fix water & steps quick-add routing

**Files to change:**
- `mobile/app/src/main/java/com/vitalai/data/remote/DashboardApi.kt`
- `mobile/app/src/main/java/com/vitalai/data/repository/DashboardRepository.kt`

**What to do:**
1. Remove `updateWater()` and `updateSteps()` from `DashboardApi` entirely.
2. In `DashboardRepository`, inject `TrainingApi` (or an `ActivityLogsApi` — see 1.1b).
3. Wire `addWater(ml: Int)` to `TrainingApi.updateWater(mapOf("logDate" to today, "waterMl" to ml))`.
4. Wire `addSteps(steps: Int)` to `TrainingApi.updateSteps(mapOf("logDate" to today, "steps" to steps))`.

**Correct backend signature (from `activity-logs.controller.ts`):**
- `PATCH activity-logs/water` — body: `{ logDate: String (ISO), waterMl: Int }`
- `PATCH activity-logs/steps` — body: `{ logDate: String (ISO), steps: Int }`

**Existing mobile file to copy pattern from:**
- `TrainingApi.kt` lines 53-57 already has the correct `updateWater`/`updateSteps` signatures
- `TrainingRepository.kt` lines 117-137 already calls them with `Map<String, Int>`
- **BUG inside TrainingRepository too**: it sends `mapOf("waterMl" to ml)` without `logDate` — add today's date

**Verification:**
- `grep -n "dashboard/water\|dashboard/steps" mobile/**/*.kt` → should return 0 results
- Tap quick-add water in HomeScreen → no 404 in logcat

---

### Task 1.2 — Fix BodyMetrics POST DTO

**Files to change:**
- `mobile/app/src/main/java/com/vitalai/data/remote/model/BodyMetricsDto.kt` (add new request class)
- `mobile/app/src/main/java/com/vitalai/data/remote/BodyMetricsApi.kt`
- `mobile/app/src/main/java/com/vitalai/data/repository/BodyMetricsRepository.kt`

**What to do:**
1. Add new data class in `BodyMetricsDto.kt`:
```kotlin
@JsonClass(generateAdapter = true)
data class UpsertBodyMetricRequest(
    @Json(name = "recorded_at") val recordedAt: String? = null,   // ISO date
    @Json(name = "weight_kg") val weightKg: Float? = null,
    @Json(name = "body_fat_pct") val bodyFatPct: Float? = null,
    @Json(name = "waist_cm") val waistCm: Float? = null,
    @Json(name = "hip_cm") val hipCm: Float? = null,
    @Json(name = "chest_cm") val chestCm: Float? = null,
    @Json(name = "neck_cm") val neckCm: Float? = null,
    @Json(name = "notes") val notes: String? = null
)
```
2. Change `BodyMetricsApi.addMetric(@Body metric: BodyMetricDto)` → `@Body metric: UpsertBodyMetricRequest`
3. Update `BodyMetricsRepository.addMetric()` call sites to build `UpsertBodyMetricRequest`

**Backend reference:** `backend/src/modules/train/dto/upsert-body-metric.dto.ts` lines 11-56

**Verification:**
- `POST body-metrics` in logcat returns 201, not 400/422
- Weight log saves successfully in MetricsScreen

---

### Task 1.3 — Fix body metrics period mapping

**Files to change:**
- `mobile/app/src/main/java/com/vitalai/ui/screens/metrics/MetricsViewModel.kt`
- `mobile/app/src/main/java/com/vitalai/ui/screens/metrics/MetricsHistoryViewModel.kt`
- Any composable that hard-codes `"1W"`, `"1M"`, `"3M"`

**What to do:**
- Replace period string literals:
  - `"1W"` → `"week"`
  - `"1M"` → `"month"`
  - `"3M"` → `"3months"`
- Add `"6months"` and `"year"` options to the period selector UI

**Backend reference:** `training.controller.ts` — `@Get('body-metrics/period/:period')` — valid values from `BodyMetricQueryDto`: `week | month | 3months | 6months | year`

**Verification:**
- `grep -n '"1W"\|"1M"\|"3M"' mobile/**/*.kt` → 0 results
- MetricsScreen period chart loads data for all 5 periods

---

### Task 1.4 — Wire auth logout to backend

**Files to change:**
- `mobile/app/src/main/java/com/vitalai/data/remote/AuthApi.kt`
- `mobile/app/src/main/java/com/vitalai/data/repository/AuthRepository.kt`

**What to do:**
1. Add to `AuthApi`:
```kotlin
@POST("auth/logout")
suspend fun logout(@Body body: Map<String, String?>): Response<ApiResponse<Unit>>
```
2. In `AuthRepository.logout()`:
```kotlin
suspend fun logout() {
    try {
        val refreshToken = tokenManager.getRefreshToken()
        authApi.logout(mapOf("refresh_token" to refreshToken))
    } catch (_: Exception) { }
    tokenManager.clearTokens()
}
```

**Backend reference:** `auth.controller.ts` `POST /auth/logout` — body: `{ refresh_token?: String }`; reads `Authorization` header automatically

**Verification:**
- After logout, calling a protected endpoint with the old access token returns 401

---

### Task 1.5 — Wire ForgotPasswordScreen to API

**Files to change:**
- `mobile/app/src/main/java/com/vitalai/ui/screens/auth/ForgotPasswordScreen.kt`
- `mobile/app/src/main/java/com/vitalai/ui/screens/auth/AuthViewModel.kt`
- `mobile/app/src/main/java/com/vitalai/data/remote/AuthApi.kt`
- `mobile/app/src/main/java/com/vitalai/data/repository/AuthRepository.kt`

**What to do:**
1. Add to `AuthApi`:
```kotlin
@POST("auth/forgot-password")
suspend fun forgotPassword(@Body body: Map<String, String>): Response<ApiResponse<Unit>>

@POST("auth/reset-password")
suspend fun resetPassword(@Body body: Map<String, String>): Response<ApiResponse<Unit>>
```
2. Add `forgotPassword(email: String)` to `AuthRepository` and `AuthViewModel`
3. Connect `ForgotPasswordScreen` submit button to `viewModel.forgotPassword(email)`
4. Add `ResetPasswordScreen` with token + newPassword fields (navigate from email deep-link)

**Backend reference:** `auth.controller.ts`  
- `POST /auth/forgot-password` body: `{ email: String }`  
- `POST /auth/reset-password` body: `{ token: String, newPassword: String }`

**Verification:**
- Enter email in ForgotPasswordScreen → gets "Email sent" response (or error for unknown email)
- No crash on submit

---

## Phase 2: Core Feature Gaps — Meal & Training
**Goal:** Complete the main tracking flows so users can manage their logged data.

### Task 2.1 — Edit food item quantity in meal log

**New endpoint:** `PATCH /meal-logs/:id/items/:itemId`  
Body: `{ quantity?: Float, serving_unit?: String }`

**Files to change:**
- `mobile/app/src/main/java/com/vitalai/data/remote/MealLogApi.kt` — add `updateItem()`
- `mobile/app/src/main/java/com/vitalai/data/repository/MealLogRepository.kt` — add `updateItem()`
- `mobile/app/src/main/java/com/vitalai/ui/screens/diary/DiaryViewModel.kt` — add `editItem()`
- `DiaryScreen.kt` or `FoodDetailScreen.kt` — add edit-quantity UI (long-press or edit icon on meal item)

**Reference DTO:** `update-meal-log.dto.ts` lines 19-30 — `UpdateMealLogItemDto { quantity?, serving_unit? }`

---

### Task 2.2 — Delete full meal log

**New endpoint:** `DELETE /meal-logs/:id` → 204

**Files to change:**
- `MealLogApi.kt` — add `deleteMealLog(id: String)`
- `MealLogRepository.kt` — add `deleteMealLog(id)`
- `DiaryViewModel.kt` — add `deleteMealLog(id)`
- `DiaryScreen.kt` — add swipe-to-delete or long-press delete on meal card header

---

### Task 2.3 — Meal log history view

**New endpoint:** `GET /meal-logs/history?fromDate=&toDate=`

**Files to change:**
- `MealLogApi.kt` — add `getMealHistory(fromDate, toDate)`
- `MealLogRepository.kt` — add `getMealHistory()`
- New: `MealHistoryScreen.kt` + `MealHistoryViewModel.kt` (or add date-range picker to DiaryScreen)

---

### Task 2.4 — Edit/rename workout session

**New endpoint:** `PATCH /training/sessions/:id`  
Body: `{ sessionDate?, sessionName?, notes? }`

**Files to change:**
- `TrainingApi.kt` — add `updateSession(id, body: UpdateSessionRequest)`
- `TrainingRepository.kt` — add `updateSession()`
- `WorkoutBuilderViewModel.kt` or new edit-session flow
- `WorkoutScreen.kt` — add edit icon on session cards

---

### Task 2.5 — Remove exercise from session

**New endpoint:** `DELETE /training/sessions/:id/exercises/:detailId` → 204

**Files to change:**
- `TrainingApi.kt` — add `removeExercise(sessionId, detailId)`
- `TrainingRepository.kt` — add `removeExercise()`
- `WorkoutBuilderViewModel.kt` or WorkoutBuilderScreen — add remove button per exercise row

---

### Task 2.6 — Log calories burned manually

**New endpoint:** `PATCH /activity-logs/calories-burned`  
Body: `{ logDate: String, caloriesBurned: Float, activeMinutes: Int, exerciseNotes?: String }`

**Files to change:**
- `TrainingApi.kt` — add `updateCaloriesBurned(body: Map<String, Any>)`
- `TrainingRepository.kt` — add `updateCaloriesBurned()`
- `ActivityScreen.kt` / `ActivityViewModel.kt` — add calories-burned input field

---

## Phase 3: Dashboard Analytics
**Goal:** Add weekly/monthly trend views.

### Task 3.1 — Weekly dashboard

**New endpoint:** `GET /dashboard/weekly?weekStart=YYYY-MM-DD`

**Files to change:**
- `DashboardApi.kt` — add `getWeeklyDashboard(weekStart: String)`
- `DashboardRepository.kt` — add `getWeeklyDashboard()`
- New: `WeeklyDashboardScreen.kt` + `WeeklyDashboardViewModel.kt`  
  OR: add weekly tab to existing `HomeScreen.kt`

---

### Task 3.2 — Monthly dashboard

**New endpoint:** `GET /dashboard/monthly?year=YYYY&month=M`

**Files to change:**
- `DashboardApi.kt` — add `getMonthlyDashboard(year, month)`
- `DashboardRepository.kt` — add `getMonthlyDashboard()`
- Integrate into a "Statistics" tab or existing profile/metrics area

---

## Phase 4: Body Progress Enhancement
**Goal:** Full body composition tracking.

### Task 4.1 — Extend weight log form with body measurements

**What to do:**
- Add `waistCm`, `hipCm`, `chestCm`, `neckCm` input fields to the metric-logging UI
- These map to `UpsertBodyMetricRequest` fields already added in Phase 1, Task 1.2

---

### Task 4.2 — Weight progress summary card

**New endpoint:** `GET /body-metrics/summary`

**Files to change:**
- `BodyMetricsApi.kt` — add `getSummary()`
- New response model: `BodyMetricsSummaryDto { weightChange, trend, currentWeight, targetWeight }`
- `MetricsViewModel.kt` — add `loadSummary()`
- `MetricsScreen.kt` — add progress card with delta (e.g. "-2.3kg this month")

---

### Task 4.3 — Progress photos

**New endpoints:**
- `GET /body-metrics/photos?limit=10`
- `POST /body-metrics/photos` (multipart: `file`, `photoType`, `bodyMetricId`)
- `DELETE /body-metrics/photos/:id`

**Files to change:**
- `BodyMetricsApi.kt` — add 3 new functions
- New model: `ProgressPhotoDto { id, photoUrl, photoType, createdAt }`
- `BodyMetricsRepository.kt` — add photo methods
- `MetricsHistoryScreen.kt` — add photo grid section and upload button (camera/gallery picker)

---

## Phase 5: Social & Blog Features
**Goal:** Enable blog engagement (likes, comments, dynamic tags).

### Task 5.1 — Blog like/unlike

**New endpoints:**
- `POST /blogs/:id/like` — toggles like
- `GET /blogs/:id/liked` → `{ liked: Boolean }`

**Files to change:**
- `BlogApi.kt` — add `toggleLike(id)`, `isLiked(id)`
- `BlogRepository.kt` — add methods
- `BlogDetailViewModel.kt` — add `toggleLike()`
- `BlogDetailScreen.kt` — add heart/like button with count

---

### Task 5.2 — Blog comments

**New endpoints:**
- `GET /blogs/:id/comments?page=&limit=`
- `POST /blogs/:id/comments` body: `{ content: String }`
- `DELETE /blogs/:id/comments/:commentId`

**New models:**
```kotlin
data class CommentDto(val id: String, val content: String, val authorUser: AuthorUserDto, val createdAt: String)
data class CommentPageDto(val items: List<CommentDto>, val total: Int, val page: Int)
data class CreateCommentRequest(val content: String)
```

**Files to change:**
- `BlogApi.kt` — add 3 comment endpoints
- `BlogRepository.kt` — add comment methods
- `BlogDetailViewModel.kt` — add `loadComments()`, `postComment()`, `deleteComment()`
- `BlogDetailScreen.kt` — add collapsible comments section at bottom

---

### Task 5.3 — Dynamic blog tags from backend

**New endpoint:** `GET /blogs/tags` → `String[]`

**Files to change:**
- `BlogApi.kt` — add `getTags()`
- `BlogRepository.kt` — add `getTags()`
- `DiscoverViewModel.kt` — replace hard-coded tags with `loadTags()` call
- `DiscoverScreen.kt` — tag chips populated from API

---

## Phase 6: Food Discovery & Custom Foods
**Goal:** Wire explore, recipe, and custom food listing.

### Task 6.1 — Food explore endpoint

**New endpoint:** `GET /foods/explore?page=&limit=&category=`

**Files to change:**
- `FoodApi.kt` — add `exploreFood(page, limit, category)`
- New model: `FoodExplorePage { items: List<FoodDto>, total, page, limit }`
- `DiscoverViewModel.kt` or new `FoodExploreViewModel.kt`
- `DiscoverScreen.kt` — add horizontal dish/recipe scroll section

---

### Task 6.2 — Recipe and ingredient detail

**New endpoints:**
- `GET /foods/:id/recipe` → `{ prep_time_min, cook_time_min, servings, steps }`
- `GET /foods/:id/ingredients` → ingredient list

**New models:**
```kotlin
data class RecipeDto(val prepTimeMin: Int?, val cookTimeMin: Int?, val servings: Int?, val steps: List<RecipeStepDto>)
data class RecipeStepDto(val stepNumber: Int, val instruction: String, val imageUrl: String?)
```

**Files to change:**
- `FoodApi.kt` — add `getRecipe(id)`, `getIngredients(id)`
- `FoodRepository.kt` — add methods
- `FoodDetailScreen.kt` — add "Recipe" tab section if food is a dish

---

### Task 6.3 — Custom food listing

**New endpoint:** `GET /foods/custom?page=&limit=`

**Files to change:**
- `FoodApi.kt` — add `getCustomFoods(page, limit)`
- `FoodViewModel.kt` or new `CustomFoodsViewModel.kt`
- `SearchFoodScreen.kt` — add "My Foods" tab showing custom foods

---

## Phase 7: AI Scan Integration
**Goal:** Wire camera scan to AI food recognition.

### Task 7.1 — AI food scan via camera

**New endpoint:** `POST /ai-scan/analyze` (multipart: `image` file)  
**Response:** `AiScanResultDto[]` → `[{ ai_food_name, estimated_weight_g, confidence_score, matched_foods }]`

**New API interface:**
```kotlin
interface AiScanApi {
    @Multipart
    @POST("ai-scan/analyze")
    suspend fun analyzeFood(@Part image: MultipartBody.Part): Response<ApiResponse<List<AiScanResultDto>>>
}
```

**New models:**
```kotlin
data class AiScanResultDto(val aiFoodName: String, val estimatedWeightG: Float, val confidenceScore: Float?, val matchedFoods: List<FoodDto>)
```

**Files to change:**
- New: `AiScanApi.kt`
- New: `AiScanRepository.kt`
- New: `AiScanViewModel.kt`
- `ScanScreen.kt` — currently only does barcode; add camera capture → analyze → show results → add to meal

**DI changes:**
- `ApiModule.kt` — add `AiScanApi` binding

---

## Phase 8: Auth Completeness & Cleanup
**Goal:** Complete auth flows, clean up dead code.

### Task 8.1 — Complete reset-password flow (already in Phase 1.5)
- `ResetPasswordScreen.kt` accepting token from deep-link + new password input

### Task 8.2 — Delete notification
**New endpoint:** `DELETE /notifications/:id` → 204

**Files to change:**
- `NotificationsApi.kt` — add `deleteNotification(id: String)`
- `NotificationsRepository.kt` — add method
- `NotificationsViewModel.kt` — add `deleteNotification()`
- `NotificationsScreen.kt` — add swipe-to-delete

---

## Execution Order

| Phase | Tasks | Priority | Effort |
|-------|-------|----------|--------|
| Phase 1 | B1–B6 bug fixes | CRITICAL | ~4h |
| Phase 2 | Meal & Training CRUD | HIGH | ~8h |
| Phase 3 | Dashboard analytics | MEDIUM | ~4h |
| Phase 4 | Body progress | MEDIUM | ~6h |
| Phase 5 | Blog engagement | MEDIUM | ~6h |
| Phase 6 | Food discovery | LOW | ~5h |
| Phase 7 | AI Scan | LOW | ~4h |
| Phase 8 | Auth cleanup | LOW | ~2h |

**Total estimated effort:** ~39 developer-hours

---

## Anti-Pattern Guards

- Do NOT invent new backend endpoints — every API call must map to a controller listed in this document
- Do NOT reuse response DTOs (like `BodyMetricDto`) as request bodies — create separate `*Request` classes
- Do NOT send requests without `logDate` to activity-log endpoints — backend requires it
- Do NOT hardcode period strings like `"1W"` — backend enum values are `week | month | 3months | 6months | year`
- Do NOT skip `@Json(name = "snake_case")` annotations on Moshi data classes — backend sends snake_case JSON
- Do NOT use `PUT` for health-profile in new screens — use the correct `PUT /users/me/health-profile` for updates, `POST /users/me/onboarding` only for first-time onboarding

---

## File Index

### Highest-change files (Phase 1):
- `DashboardApi.kt` — remove wrong endpoints
- `DashboardRepository.kt` — reroute water/steps to TrainingApi
- `BodyMetricsApi.kt` — fix addMetric DTO
- `BodyMetricsDto.kt` — add UpsertBodyMetricRequest
- `AuthApi.kt` — add logout, forgotPassword, resetPassword
- `AuthRepository.kt` — wire logout + forgot password
- `MetricsViewModel.kt` — fix period strings
- `ForgotPasswordScreen.kt` — wire to viewModel

### New files to create:
- `AiScanApi.kt`, `AiScanRepository.kt`, `AiScanViewModel.kt`
- `UpsertBodyMetricRequest` (inside BodyMetricsDto.kt)
- `CommentDto`, `CreateCommentRequest` (inside BlogDto.kt)
- `ResetPasswordScreen.kt`
- `WeeklyDashboardViewModel.kt` (or extend HomeViewModel)
