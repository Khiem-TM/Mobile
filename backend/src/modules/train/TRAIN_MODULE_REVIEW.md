# Train Module — Kiểm thử & Đánh giá

> **Ngày đánh giá:** 2026-05-29  
> **Phiên bản:** `main` branch, commit `787e5a8`  
> **Phạm vi:** `backend/src/modules/train/**`  
> **Người đánh giá:** Claude Sonnet 4.6 (automated review)

---

## 1. Tổng quan cấu trúc

```
train/
├── controllers/       5 files  (exercises, training-sessions, body-metrics, activity-logs, favorite-exercises)
├── services/          6 files  (+ calories-calculation)
├── repositories/     11 files  (5 interfaces + 6 implementations)
├── entities/          7 files
├── dto/               9 files
├── enums/             3 files
├── mappers/           1 file
└── tests/             1 file   (training-sessions.service.spec.ts)
```

**Công nghệ:** NestJS, TypeORM, PostgreSQL, Cloudinary, JWT Auth  
**Tổng số file:** ~46 files

---

## 2. Điểm mạnh ✅

### 2.1 Kiến trúc phân lớp rõ ràng
Controller → Service → Repository → Entity được tuân thủ tốt. Controller không chứa business logic, không tính toán calories. `CaloriesCalculationService` được tách riêng hoàn toàn.

### 2.2 Repository Pattern với Interface abstraction
Mỗi repository đều có interface (`ITrainingSessionsRepository`, `IBodyMetricsRepository`, ...) và được inject qua DI token (`TRAINING_SESSIONS_REPOSITORY`). Thiết kế này hỗ trợ testability cao và có thể swap implementation.

### 2.3 Security cơ bản đúng
- `userId` luôn lấy từ JWT payload (`@CurrentUser()`) — không tin từ request body
- `ForbiddenException` khi user cố truy cập dữ liệu của user khác
- `JwtAuthGuard` bảo vệ toàn bộ protected endpoints
- TypeORM parameterized queries — không có SQL injection risk

### 2.4 Calorie calculation đúng hướng
Ba công thức tách biệt rõ ràng:
- **MET-based:** `calories = MET × weight × hours` — ưu tiên cao nhất
- **SPORT intensity-based:** `calories = caloriesPerMin × duration`
- **GYM factor-based:** `calories = (factor × weight × duration) / 60`
- **CARDIO:** tính `duration = distance / speed` → áp MET lookup theo tốc độ

Fallback chain cho user weight: `latestBodyMetric → healthProfile.initialWeightKg → 70kg`

### 2.5 Transaction cho FavoriteExercise
`addFavorite` và `removeFavorite` dùng `DataSource.transaction` để đảm bảo atomicity giữa insert/delete record và increment/decrement `favoritesCount`.

### 2.6 Streak integration
`refreshWorkoutStreak` được gọi sau `createSession`, `deleteSession`, và `updateSession` (khi đổi date). Streak được tính lại từ tất cả session dates thực tế thay vì cộng/trừ đơn giản — chính xác hơn.

### 2.7 Response normalization qua Mapper
`toExerciseMobileDto` chuẩn hóa Exercise response cho mobile client, xử lý fallback cho `caloriesPerMin` và expose cả `primaryMuscleGroup` lẫn `muscleGroup`.

---

## 3. Lỗi & Vấn đề tìm thấy 🔴

### 3.1 [CRITICAL] `createSession` thiếu transaction — dữ liệu có thể corrupt

**File:** `services/training-sessions.service.ts:160-176`

```typescript
// Không có transaction wrapping
const session = await this.sessionRepo.createSession({ ...totals });

for (const itemData of itemDataList) {
  await this.sessionRepo.addItem({ ...itemData, sessionId: session.id }); // ← nếu lần 3 fail?
}

await this.refreshWorkoutStreak(userId); // ← chạy dù items chưa đủ
```

**Vấn đề:**
- Session được tạo với `totalCaloriesBurned` đúng, nhưng nếu `addItem` thứ N fail, session tồn tại với N-1 items nhưng tổng calories vẫn là của N items
- Streak được refresh ngay cả khi session chưa complete
- Không có rollback

**Fix:** Dùng `QueryRunner` hoặc `DataSource.transaction` bọc toàn bộ flow: createSession + addItems + updateTotals + refreshStreak.

---

### 3.2 [CRITICAL] `addFavorite` với `orIgnore()` vẫn increment favoritesCount

**File:** `services/favorite-exercises.service.ts` (dựa trên repository description)

Flow hiện tại:
```
1. INSERT INTO favorite_exercises ... ON CONFLICT DO NOTHING
2. UPDATE exercises SET favorites_count = favorites_count + 1
```

Nếu user gọi `POST /favorite-exercises/:id` hai lần liên tiếp:
- Lần 1: insert thành công + count tăng lên 1 ✅
- Lần 2: insert bị orIgnore (không insert) nhưng count vẫn tăng lên 2 ❌

**Fix:** Kiểm tra affected rows sau insert, chỉ increment nếu `result.affected > 0`.

---

### 3.3 [BUG] Dead code trong `updateItem` — else if không bao giờ chạy

**File:** `services/training-sessions.service.ts:297-321`

```typescript
const needsRecalc =
  dto.durationMinutes !== undefined ||  // ← durationMinutes đã check ở đây
  dto.intensityLevel !== undefined ||
  // ...

if (needsRecalc) {
  // recalculate...
} else if (dto.durationMinutes !== undefined) { // ← DEAD CODE: không bao giờ đúng
  updateData.durationMinutes = dto.durationMinutes;
}
```

Vì `dto.durationMinutes !== undefined` đã được include trong `needsRecalc`, nhánh `else if` không bao giờ được thực thi. Nếu user chỉ cập nhật `durationMinutes`, code sẽ vào nhánh `if (needsRecalc)` và gọi `resolveItemCaloriesAndDuration` — hành vi đúng, nhưng code dễ gây nhầm lẫn.

**Fix:** Xóa nhánh `else if` thừa.

---

### 3.4 [BUG] `addItem` không dùng transaction khi cập nhật totals

**File:** `services/training-sessions.service.ts:264-269`

```typescript
await this.sessionRepo.addItem({ ... });   // DB write 1

const totals = await this.recalcSessionTotals(sessionId);  // DB read
await this.sessionRepo.updateTotals(...);  // DB write 2  ← window này có thể stale
```

Giữa `addItem` và `updateTotals`, nếu có concurrent request khác thêm item vào cùng session, `recalcSessionTotals` sẽ đọc được state mới nhưng sau đó `updateTotals` có thể overwrite bằng stale value.

---

### 3.5 [BUG] `getProgressSummary` sort in-memory với 1000 records

**File:** `services/body-metrics.service.ts:130-151`

```typescript
const history = await this.repository.findHistory(userId, { limit: 1000 }); // ← tải 1000 records
if (history.length === 0) { ... }

const sorted = [...history].sort(  // ← sort in-memory
  (a, b) => new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime(),
);
```

Hàm này chỉ cần first record (earliest) và last record (latest), nhưng tải tối đa 1000 records rồi sort trong memory. User có >1000 records thì `first` có thể sai (vì `findHistory` có thể sort theo DESC, không phải ASC).

**Fix:** Thêm 2 repo queries: `findOldest(userId)` và `findLatest(userId)`, hoặc dùng `ORDER BY measuredAt ASC LIMIT 1` + `ORDER BY measuredAt DESC LIMIT 1`.

---

### 3.6 [PERFORMANCE] `refreshWorkoutStreak` tải toàn bộ session history

**File:** `services/training-sessions.service.ts:105-108`

```typescript
const dates = await this.sessionRepo.findDistinctSessionDates(userId); // ALL dates, no limit
return this.streaksService.recomputeFromActivityDates(userId, StreakType.WORKOUT, dates);
```

`findDistinctSessionDates` trả toàn bộ session dates trong lịch sử user (không có LIMIT). Gọi sau mỗi create/delete/update session. User tập gym 2 năm = ~700 dates phải tải mỗi lần.

**Fix:** Streak chỉ cần recent dates để tính current streak. Giới hạn trong ~90 ngày gần nhất, hoặc cache streak và chỉ invalidate khi cần.

---

### 3.7 [PERFORMANCE] `updateItem` gọi `getUserWeight` trên mỗi recalc

**File:** `services/training-sessions.service.ts:310`

Mỗi khi update một item, `getUserWeight` gọi 2 queries: `getLatest(userId)` → `getHealthProfile(userId)`. Nếu batch update nhiều items, mỗi item tốn 2 DB round-trips để lấy cùng một giá trị.

---

## 4. Vấn đề về Code Quality 🟡

### 4.1 Return type `any` khắp nơi

**File:** `services/training-sessions.service.ts`

```typescript
async createSession(userId: string, dto: CreateTrainingSessionDto): Promise<any>  // ← any
async getSessions(userId: string, limit = 20): Promise<any[]>                     // ← any[]
async addItem(...): Promise<any>                                                   // ← any
```

Tất cả service methods trong TrainingSessionsService trả `any`. Mất đi type safety, IDE không có autocomplete. Nên định nghĩa `TrainingSessionResponse` interface hoặc DTO.

### 4.2 Duplicate logic giữa `createSession` và `addItem`

**File:** `services/training-sessions.service.ts:138-154` vs `246-262`

Code xây dựng item data (sets fallback, reps fallback, restTimeSeconds fallback theo exerciseType) bị duplicate hoàn toàn giữa `createSession` và `addItem`. Nên extract thành private `buildItemData(dto, exercise)`.

### 4.3 Swagger documentation thiếu `@ApiResponse`

**File:** `controllers/*.controller.ts`

Tất cả endpoints chỉ có `@ApiOperation` và một số `@ApiQuery`, không có `@ApiResponse`. Swagger UI hiện tại không show response schema cho bất kỳ endpoint nào.

### 4.4 Thiếu date format validation cho route params và query strings

```typescript
// controllers/training-sessions.controller.ts
@Get('date/:date')
getSessionsByDate(@Param('date') date: string) // ← không validate format
```

String `date` được truyền thẳng vào DB query. Input `'2026-13-99'` hay `'not-a-date'` sẽ gây DB error thay vì 400 BadRequest. Nên dùng `@IsDateString()` trong DTO hoặc custom pipe.

### 4.5 `ActivityLog` entity thiếu `@ManyToOne` relation

**File:** `entities/activity-log.entity.ts`

Entity chỉ có raw column `userId` nhưng không có `@ManyToOne(() => User)` relation. Không thể eager load User từ ActivityLog. Ngược lại, `TrainingSession` entity lại có đầy đủ `@ManyToOne`. Inconsistency giữa các entities.

### 4.6 `secondaryMuscleGroups` dùng `simple-json` thay vì `text array`

**File:** `entities/exercise.entity.ts:75`

```typescript
@Column({ name: 'secondary_muscle_groups', type: 'simple-json', nullable: true })
secondaryMuscleGroups!: string[] | null;
```

`imageUrl` và `imagePublicIds` dùng `type: 'text', array: true` (native PG array), nhưng `secondaryMuscleGroups` dùng `simple-json` (JSON string). Không nhất quán và `simple-json` mất khả năng query trực tiếp từ DB.

### 4.7 Magic comment trong entity file

**File:** `entities/activity-log.entity.ts:29`, `entities/exercise.entity.ts:87`

```typescript
// Thêm lời nhắn
note!: string | null;

// ─── GYM-specific fields ─────────────────────────────────────────────────
```

Comment `// Thêm lời nhắn` không mô tả được ý nghĩa business. Comment phân cách section bằng dashes dài là style cá nhân không cần thiết.

---

## 5. Gap so với Yêu cầu (CLAUDE.md) 🟠

| Yêu cầu từ CLAUDE.md | Trạng thái |
|---|---|
| `POST /exercises` — tạo exercise | ❌ Chưa có |
| `PATCH /exercises/:id` — cập nhật exercise | ❌ Chưa có |
| `DELETE /exercises/:id` — xóa exercise | ❌ Chưa có |
| `GET /exercises/type/:type` — filter by type | ⚠️ Filter qua query param `?type=` thay vì path param |
| `GET /activity-logs/statistics` | ❌ Chưa có |
| `GET /body-metrics/statistics` | ⚠️ Có `/summary` nhưng không đủ fields như mô tả |
| CARDIO là type thứ 3 (ngoài GYM/SPORT) | ✅ Đã implement (entity modified) |
| Dashboard tổng hợp từ train module | ⚠️ Nằm ở DashboardModule riêng |
| Pagination cho Exercise | ✅ Có page/limit |
| Date range filter cho Training Session | ✅ Có |
| Calories tính backend, không tin client | ✅ Đúng |
| FavoriteExercise dùng transaction | ✅ Đúng |
| `TrainingSession` unique per day | ✅ Có unique constraint |
| `ActivityLog` unique per day | ✅ Có unique constraint |
| Upload progress photos | ✅ Đã implement |
| Body progress photo có photoType | ✅ Có (front/back/side) |

---

## 6. Test Coverage 🔴

**Trạng thái hiện tại:** Cực kỳ thiếu

| Component | Tests |
|---|---|
| ExercisesService | ❌ 0 tests |
| TrainingSessionsService | ⚠️ 1 test (estimateGymDurationMinutes) |
| CaloriesCalculationService | ❌ 0 tests |
| BodyMetricsService | ❌ 0 tests |
| ActivityLogsService | ❌ 0 tests |
| FavoriteExercisesService | ❌ 0 tests |
| Repositories | ❌ 0 tests |
| Controllers | ❌ 0 tests |

**Test duy nhất:** `training-sessions.service.spec.ts` test hàm `estimateGymDurationMinutes` — một private helper function.

**Các case cần test khẩn cấp:**
- `CaloriesCalculationService`: tất cả 3 công thức, edge cases (MET=0, weight fallback)
- `createSession`: item fail midway → session không tồn tại
- `addFavorite` twice → favoritesCount không bị tăng 2 lần
- `updateItem` với chỉ `durationMinutes` → recalc đúng
- `getProgressSummary` với user có 0 records, 1 record, nhiều records

---

## 7. Bảng tóm tắt mức độ ưu tiên

| # | Vấn đề | Mức độ | Effort |
|---|---|---|---|
| 1 | `createSession` thiếu transaction | 🔴 Critical | Medium |
| 2 | `addFavorite` + `orIgnore` vẫn increment | 🔴 Critical | Low |
| 3 | Dead code trong `updateItem` else if | 🟡 Minor | Low |
| 4 | `addItem` không atomic khi update totals | 🟠 High | Medium |
| 5 | `getProgressSummary` load 1000 records | 🟠 High | Low |
| 6 | `refreshWorkoutStreak` tải toàn bộ history | 🟠 High | Medium |
| 7 | Return type `any` khắp service | 🟡 Medium | High |
| 8 | Duplicate item build logic | 🟡 Medium | Low |
| 9 | Thiếu date format validation | 🟡 Medium | Low |
| 10 | Swagger thiếu `@ApiResponse` | 🟡 Medium | High |
| 11 | Test coverage gần như 0% | 🟠 High | High |
| 12 | Admin CRUD endpoints cho Exercise | 🟠 High | Medium |
| 13 | `secondaryMuscleGroups` dùng simple-json | 🟡 Low | Low |

---

## 8. Khuyến nghị hành động

### Ưu tiên ngay (Sprint hiện tại)

1. **Fix `addFavorite` race condition:** Kiểm tra `result.affected > 0` trước khi increment.
2. **Wrap `createSession` trong transaction:** Dùng `DataSource.transaction` bao gồm createSession + tất cả addItem + updateTotals.
3. **Fix `getProgressSummary`:** Thay `findHistory(limit: 1000)` bằng 2 targeted queries.

### Ưu tiên trung hạn

4. **Viết unit tests cho `CaloriesCalculationService`** — đây là core business logic, phải có tests.
5. **Thêm date format validation** cho tất cả date params và query strings.
6. **Thêm Admin CRUD endpoints cho Exercise** (`POST`, `PATCH`, `DELETE /exercises`).

### Cải thiện dài hạn

7. **Định nghĩa Response Types** thay vì `any` trong service layer.
8. **Optimize `refreshWorkoutStreak`** — chỉ tính từ recent dates hoặc cache.
9. **Thêm `@ApiResponse` decorators** cho tất cả endpoints.
10. **Extract `buildItemData` helper** để loại bỏ code duplication giữa `createSession` và `addItem`.

---

## 9. Kết luận tổng thể

Train Module được thiết kế **đúng hướng và có tư duy kiến trúc tốt**: phân lớp rõ ràng, security đúng chỗ, CaloriesCalculationService tách biệt, FavoriteExercise dùng transaction. Đây là nền tảng solid để phát triển tiếp.

Tuy nhiên, **2 lỗi nghiêm trọng** (thiếu transaction trong createSession, favoritesCount có thể tăng sai) cần fix trước khi production. Test coverage gần như bằng 0 là rủi ro lớn nhất khi refactor hoặc mở rộng.

**Điểm đánh giá:**
- Kiến trúc / Design: **8/10**
- Correctness / Bugs: **5/10**
- Performance: **6/10**
- Security: **8/10**
- Test Coverage: **2/10**
- Code Quality: **6/10**
- Documentation: **5/10**
