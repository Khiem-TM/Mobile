# Mobile E2E Fix Report — VitalAI (Kotlin Android ↔ NestJS)

_Generated 2026-05-30. Scope: make the Kotlin app work end-to-end with the NestJS backend, remove production hardcode, and align the UI with `Design/DESIGN.md`._

## 1. Tổng quan vấn đề ban đầu
The app (package `com.vitalai`, Compose + Hilt + Retrofit/Moshi + Coil) was largely wired to the backend (10 Retrofit interfaces, base URL `http://10.0.2.2:3005/` matching backend `PORT=3005`, no global prefix, responses wrapped `{success, statusCode, data}`). But images never rendered, the dashboard avatar was hardcoded, the "create food" image picker did nothing, adding a 2nd workout in a day failed, there was no goals screen, and the theme didn't match the design system. **No backend changes were needed** — all fixes are mobile-side.

## 2. Các lỗi chính & Root cause

| # | Problem | Root cause | Severity |
|---|---------|-----------|----------|
| 1 | Thumbnails/avatars/exercise images don't render | Backend serves seed images as **relative paths** (`/seed-images/foods/x.svg`) and as **SVG**. Coil received a bare relative string (couldn't resolve) and had **no SVG decoder**. | P0 |
| 2 | Can't add a 2nd training session in a day | Backend enforces one session/day → **HTTP 409**; mobile always POSTed create with `LocalDate.now()` and surfaced the raw failure. | P0 |
| 3 | Create personal food incomplete | Image box was a no-op (`/* Handle Image Upload */`); no upload, no error surfacing, list not refreshed. | P1 |
| 4 | No "Mục tiêu của tôi" screen | Screen never built; profile menu item navigated to an empty lambda. | P1 |
| 5 | Hardcoded dashboard data | Avatar fell back to `https://i.pravatar.cc/...`; meal "times" were fake (`8:30 AM`/`12:45`/`15:20`). | P1 |
| 6 | UI not aligned to DESIGN.md | Primary was Mint `#10B981` on white; design wants Forest Green `#0f3e17` on Cream `#fffefc`, 14dp radius. HomeScreen had no error state / pull-to-refresh. | P2 |

### Note — a hypothesis that turned out FALSE
The initial audit suspected the **dashboard JSON was unparseable** (flat mobile DTO vs nested backend). On inspection the data layer was already correct: `DashboardApi.getDashboard` returns the nested `DailyDashboardResponse`, and `DashboardRepository` maps it to the flat UI `DashboardDto`, merging goals from the health-profile. No change was required there beyond UX (error state / refresh).

## 3. Cách đã sửa (by phase)

**Phase 1 — Centralized image loading**
- Added `io.coil-kt:coil-svg:2.6.0`.
- New `core/network/ImageUrlResolver.kt`: passes absolute URLs through; prefixes relative paths with `BuildConfig.BASE_URL`.
- `VitalAIApp` now implements `ImageLoaderFactory` and registers `SvgDecoder.Factory()` app-wide.
- Routed image getters through the resolver at the DTO boundary: `FoodDto.imageUrl`, `FoodBriefDto.imageUrl`, `ExerciseDto.displayImageUrl` (now also falls back to gallery `imageUrl[0]`), `UserDto.avatarUrl`.

**Phase 2 — Dashboard UX + de-hardcode**
- `HomeViewModel`: surfaces a real error (via `AppErrorMapper`) only when the dashboard fails with no data; added `isRefreshing` + `refresh()`; keeps prior data on partial failures.
- `HomeScreen`: added `ErrorState` + retry, **pull-to-refresh** (m2 `pullRefresh`), and **resume-reload** so goal edits reflect on return. Avatar now uses an **initial-letter circle** fallback instead of pravatar. Meal cards show food names + a meal-period label instead of fake clock times.

**Phase 3 — Training merge-into-today**
- `TrainingRepository.createSession`: on **409**, fetches the day's existing session and appends each item via `POST /training-sessions/{id}/items`, then returns the refreshed session.
- `WorkoutScreen`: reloads history on `ON_RESUME` so a saved/merged session shows immediately.

**Phase 4 — Create personal food end-to-end**
- Extracted the avatar image-picker helper into shared `util/ImagePicker.kt` (`PickedImage`, `copyUriToCacheFile`); ProfileScreen now reuses it.
- `FoodApi.uploadFoodImage` (multipart `POST /foods/{id}/image`, field `file`); `FoodRepository.createFood` optionally uploads the image after create and parses backend error bodies; `uploadFoodImage` added.
- `CreateFoodScreen`: wired `PickVisualMedia`, shows a preview, passes the file to the VM, validates name+calo, shows errors via Toast. `FoodViewModel.createFood` refreshes "Món của tôi" on success.

**Phase 5 — Goals screen**
- New `ui/screens/goals/GoalsViewModel.kt` + `GoalsScreen.kt`: loads `GET /users/me/health-profile`, edits daily calories / protein / carbs / fat / water / target weight / weekly rate / goal type, validates, and saves via `PUT /users/me/health-profile` (preserving non-goal fields). Added `Screen.Goals` route, registered in `NavGraph`, wired the profile menu item.

**Phase 6 — Design re-theme**
- `ui/theme/Color.kt`: added DESIGN palette (`ForestGreen`, `CreamCanvas`, `MintGlaze`, `KeylimeWash`, `MintKiss`, `SlateMist`, `BorderGrey`, `InkText`, `DarkCharcoal`). Mapped `Mint500 → ForestGreen` (the app-wide action color), light steps → green tints, `AppBackground/AppSurface → Cream`, `AppLine → BorderGrey`, `Ink900 → #222222`, `Ink800 → #333333`. The Material color scheme references these tokens, so primary/background propagate automatically.
- `VitalDesignSystem`: `VitalCard`/`VitalFlatCard`/`VitalButton` radius → **14dp**.

## 4. API mapping đã kiểm tra
- **Auth/User**: `users/me`, `users/me/health-profile` (GET/PUT) — field names match `HealthProfileDto`. Avatar `avatar_url` full Cloudinary URL.
- **Dashboard**: `GET /dashboard` nested response correctly mapped; goals merged from health-profile. `weekly`/`monthly` parse (unknown extra fields ignored by Moshi).
- **Food**: `POST /foods` body matches `CreateFoodDto` (snake_case); image upload field is `file`; `image_urls` relative seed paths now normalized.
- **Train**: `POST /training-sessions` (`sessionDate`, `items[]` with `exerciseId`+`exerciseType`) matches; one-session-per-day 409 handled; `POST /training-sessions/{id}/items` used for merge. Exercise images `imageAvtUrl`/`imageUrl[]` normalized.

## 5. Endpoint/contract mismatches found
- **None blocking.** Backend dashboard returns a nested shape (correct/richer) and goals live in the health-profile (no dedicated goals endpoint) — both handled on mobile by design.

## 6. Files changed
**New:** `core/network/ImageUrlResolver.kt`, `di`-free `util/ImagePicker.kt`, `ui/screens/goals/GoalsViewModel.kt`, `ui/screens/goals/GoalsScreen.kt`.
**Modified:** `app/build.gradle.kts`; `VitalAIApp.kt`; DTOs `FoodDto.kt`, `MealLogDto.kt`, `TrainingDto.kt`, `AuthDto.kt`; `data/remote/FoodApi.kt`; `data/repository/{FoodRepository,TrainingRepository}.kt`; `ui/screens/home/{HomeViewModel,HomeScreen}.kt`; `ui/screens/diary/{FoodViewModel,CreateFoodScreen}.kt`; `ui/screens/workout/WorkoutScreen.kt`; `ui/screens/profile/ProfileScreen.kt`; `navigation/{Screen,NavGraph}.kt`; `ui/theme/Color.kt`; `ui/components/VitalDesignSystem.kt`.

## 7. Chạy lại app
```bash
# Backend (port 3005, seeded DB + Redis running)
cd backend && npm run start:dev
# Mobile (emulator reaches host backend at 10.0.2.2:3005)
cd mobile && ./gradlew assembleDebug   # ✅ BUILD SUCCESSFUL
# install: ./gradlew installDebug
```
To point at another host: set `BASE_URL` in `mobile/local.properties` or env `VITALAI_BASE_URL`.

## 8. Test các flow chính
1. **Images**: open food list / exercise library → seed SVG thumbnails render; profile/dashboard avatar shows photo or initial.
2. **Dashboard**: real calories/macros/water/steps + goals; pull-to-refresh; airplane mode → error state + "Thử lại".
3. **Training**: add a workout twice the same day → exercises append to today's session; appears in history.
4. **Create food**: pick image + fill name/calo → save → appears in "Món của tôi" with thumbnail; invalid input → friendly error.
5. **Goals**: Profile → "Mục tiêu & kế hoạch" → edit → save → dashboard rings reflect new targets.

## 9. Vấn đề còn tồn đọng / Next steps
- **Manual E2E not run here** (no emulator/device in this environment); build verified via `assembleDebug`. Run the flows above on a device against the seeded backend.
- **Release base URL** is still the `https://api.vitalai.invalid/` placeholder — set a real value before release.
- **State consistency**: HomeScreen now has error + refresh; some secondary screens (e.g. SearchFood) still lack an explicit error state — low priority polish.
- **Design pass**: tokens are migrated; a visual sweep of dark-on-dark edge cases (screens that hardcode colors) is worth a designer review.
- **Manual activity calories** (TrainingRepository SharedPreferences) remain local-only — backend has no endpoint; document if persistence is required.

## 10. Round 2 — bugs found during on-device testing (2026-05-30)

Five further defects surfaced once the app ran against the live backend. All were **mobile-side contract mismatches**; no backend change needed.

| # | Symptom | Root cause | Fix |
|---|---------|-----------|-----|
| R1 | Notifications crash: `Required value 'message' missing at $.data[1]` | The `notifications` entity column is **`body`**, but `NotificationDto` mapped `@Json(name = "message")`. | Map the field to `@Json(name = "body")` (Kotlin property still `message`). |
| R2 | Dashboard crash: `Expected BEGIN_OBJECT but was BEGIN_ARRAY at $.data.streaks` | `DashboardService` returns `streaks` as an **array** of per-type rows; `DailyDashboardResponse.streaks` was typed as a single `StreakDto`. | Retype to `List<DailyStreakDto>?` (new `DailyStreakDto`). |
| R3 | Streak counter on Home/Profile always 0 (silent) | `GET /streaks` also returns an **array** (`Streak[]`), but `DashboardApi.getStreaks()` expected `ApiResponse<StreakDto>` — a shape the backend never emits. Parse failed and was swallowed. | Return `List<DailyStreakDto>`; `DashboardRepository.getStreaks()` flattens by `streak_type` (`login`/`calorie_goal`/`workout`) into the UI's `StreakDto`. |
| R4 | Can't delete a meal-diary item (delete silently failed) | `MealLogApi.deleteItem` was typed `Response<ApiResponse<Unit>>`. **Moshi has no adapter for `kotlin.Unit`**, so Retrofit threw while building the converter → caught as a generic failure. Same latent bug on `markAllRead`, `deleteCustomFood`, `addFavorite`, `removeFavorite`. | Switch all 204-No-Content endpoints to plain `Response<Unit>` (the pattern `deleteMealLog`/`deleteSession` already used). |
| R5 | Blog thumbnails / in-article images not rendering | Blog `thumbnailUrl` and block `imageUrl` were passed to Coil **raw**, bypassing the central `ImageUrlResolver` (relative/SVG seed paths never resolved). | Added resolved getters `BlogDto.thumbnailImage` and `BlogBlockDto.displayImageUrl`; display sites now use them (absolute Cloudinary URLs pass through unchanged). |

Re-verified: `./gradlew assembleDebug` → **BUILD SUCCESSFUL**.

## 11. Round 3 — Train UX polish (2026-05-30)

The save-session API works (kcal shows on the dashboard), but the Train flow had UX/logic defects. All mobile-only.

| # | Symptom | Root cause | Fix |
|---|---------|-----------|-----|
| T1 | Saving a session felt abrupt, no confirmation | `WorkoutBuilderScreen` just `popBackStack()` on save; errors were swallowed. | Toast **"Đã lưu buổi tập 💪"** then pop; also toast on save error. |
| T2 | "Xem lại buổi tập" (recent history) showed wrong content | `Screen.WorkoutSession` was a **NavGraph placeholder that re-rendered `WorkoutScreen`**, and history rows navigated to the **builder** (a fresh empty session). | New **`WorkoutSessionScreen` + `WorkoutSessionViewModel`** (read-only detail: stats + exercise list, loaded via `getSessions(date)` + match by id). Route now carries `date`; history rows navigate to `WorkoutSession(id, date)`. |
| T3 | Activity log duplicated/awkward inside Train | Steps/water update lived in both Train (`ActivityLogRow`, `TodayActivityMini`, steps dialog) and Home. | Removed the activity UI from `WorkoutScreen`; **consolidated on Home**. `WaterAndActivityCards` now has explicit **tap-to-add-water (+250ml)** and **tap-to-update-steps dialog** (replaced the hidden double-click). `HomeViewModel.addWater` is now additive; `addSteps`→`setSteps` (absolute). |

Re-verified: `./gradlew assembleDebug` → **BUILD SUCCESSFUL**.

## Production-readiness checklist (mobile)
- [x] Images render from backend (relative + SVG)
- [x] Dashboard shows real user data, no hardcoded avatar
- [x] Create personal food works end-to-end (incl. image)
- [x] Add training session works (merge one-per-day)
- [x] Goals view/edit screen wired to backend
- [x] Error/loading/empty/refresh on the main dashboard
- [x] UI tokens aligned to DESIGN.md (Forest Green / Cream / 14dp)
- [x] `./gradlew assembleDebug` passes
- [ ] On-device E2E pass against seeded backend
- [ ] Real release `BASE_URL`
