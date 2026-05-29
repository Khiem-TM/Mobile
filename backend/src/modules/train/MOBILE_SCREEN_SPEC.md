# Train Module — Đặc tả thiết kế màn hình Mobile

> **Phiên bản:** 1.0  
> **Ngày:** 2026-05-29  
> **Phạm vi:** 8 màn hình thuộc Train Module  
> **Backend base URL:** `/api` (JWT Bearer required cho tất cả)

---

## Mục lục

1. [workout_home](#1-workout_home)
2. [exercise_library](#2-exercise_library)
3. [strength_detail](#3-strength_detail--bài-tập-gym)
4. [sport_detail](#4-sport_detail--bài-tập-sport)
5. [cardio_detail](#5-cardio_detail--bài-tập-cardio)
6. [session_workout](#6-session_workout)
7. [body_metric](#7-body_metric)
8. [activity_log](#8-activity_log)

---

## Quy ước chung

| Ký hiệu | Nghĩa |
|---|---|
| `R` | Required — luôn hiển thị |
| `O` | Optional — hiển thị nếu có data |
| `C` | Computed — tính từ data khác, không nhập tay |
| `→ API` | Endpoint backend tương ứng |

---

## 1. workout_home

**Mục đích:** Màn hình tổng quan luyện tập. Entry point chính của Train Module. Hiển thị tiến độ hôm nay, streak, lịch sử gần đây và shortcut khởi động session.

### Layout

```
┌──────────────────────────────────────┐
│  🔥 Streak: 7 ngày liên tiếp         │  ← streak banner
├──────────────────────────────────────┤
│  Hôm nay · Thứ Sáu, 29/05/2026      │
│  ┌─────────────┐  ┌─────────────┐   │
│  │  420 kcal   │  │   75 phút   │   │  ← today summary cards
│  │  Đã đốt     │  │   Tập luyện │   │
│  └─────────────┘  └─────────────┘   │
├──────────────────────────────────────┤
│  Biểu đồ Calories đốt (7 ngày)      │  ← weekly bar chart
│  ▁▃▅▇▃▅█                            │
├──────────────────────────────────────┤
│  [  + Bắt đầu buổi tập hôm nay  ]   │  ← CTA button
├──────────────────────────────────────┤
│  Lịch sử gần đây                    │
│  ┌──────────────────────────────┐   │
│  │ 28/05 · Push Day · 420 kcal  │   │
│  │ 27/05 · Leg Day  · 380 kcal  │   │
│  │ 25/05 · Cardio   · 300 kcal  │   │
│  └──────────────────────────────┘   │
└──────────────────────────────────────┘
```

### Data Fields

#### Section: Streak Banner
| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `currentStreak` | `number` | `R` | Số ngày liên tiếp có buổi tập |
| `longestStreak` | `number` | `O` | Kỷ lục chuỗi dài nhất |

#### Section: Today Summary
| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `todayDate` | `string` | `C` | Format: `"Thứ Sáu, 29/05/2026"` |
| `todayCaloriesBurned` | `number` | `C` | Tổng từ session hôm nay |
| `todayDurationMinutes` | `number` | `C` | Tổng duration từ session hôm nay |
| `hasSessionToday` | `boolean` | `C` | Dùng để toggle CTA |

#### Section: Weekly Chart (7 ngày)
| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `weeklyData[].date` | `string` | `R` | Format `YYYY-MM-DD` |
| `weeklyData[].caloriesBurned` | `number` | `R` | Tổng calories của ngày |
| `weeklyData[].durationMinutes` | `number` | `O` | Tổng thời gian |
| `weeklyData[].hasSession` | `boolean` | `C` | Để tô màu ngày có tập |

#### Section: Lịch sử gần đây (list)
| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `sessions[].id` | `string` | `R` | UUID |
| `sessions[].sessionDate` | `string` | `R` | Format `DD/MM` |
| `sessions[].title` | `string \| null` | `O` | Fallback: `"Buổi tập"` |
| `sessions[].totalCaloriesBurned` | `number` | `R` | |
| `sessions[].totalDurationMinutes` | `number` | `R` | |
| `sessions[].itemCount` | `number` | `C` | `items.length` |

### API Calls

```
GET /training-sessions?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD   → 7 ngày gần nhất
GET /training-sessions?limit=10                                  → recent list
```

### Actions
- Tap **[+ Bắt đầu buổi tập]** → navigate to `session_workout` (tạo mới)
- Tap session card → navigate to `session_workout` (xem chi tiết session đã có)
- Pull-to-refresh → reload toàn bộ

### Empty State
- Chưa có session nào: hiển thị illustration + `"Hãy bắt đầu buổi tập đầu tiên của bạn!"`
- Streak = 0: không hiển thị streak banner

---

## 2. exercise_library

**Mục đích:** Thư viện bài tập. Tìm kiếm, lọc, xem danh sách theo type/category/difficulty. Dùng khi thêm bài tập vào session hoặc browse.

### Layout

```
┌──────────────────────────────────────┐
│  🔍 [Tìm kiếm bài tập...          ] │
├──────────────────────────────────────┤
│  [Tất cả] [GYM] [SPORT] [CARDIO]    │  ← type filter tabs
│  [Tất cả] [Ngực] [Lưng] [Chân] ...  │  ← category filter chips
│  [Tất cả] [Dễ] [Trung bình] [Khó]   │  ← difficulty chips
├──────────────────────────────────────┤
│  Phổ biến nhất                       │
│  ┌───┬────────────────────────────┐  │
│  │ 🖼 │ Bench Press            ♥ 120│  │
│  │   │ GYM · Ngực · Trung bình    │  │
│  └───┴────────────────────────────┘  │
│  ┌───┬────────────────────────────┐  │
│  │ 🖼 │ Running               ♥ 98 │  │
│  │   │ CARDIO · Toàn thân · Dễ   │  │
│  └───┴────────────────────────────┘  │
│  ...                                  │
├──────────────────────────────────────┤
│  [  ❤️ Bài tập yêu thích của tôi  ] │  ← shortcut tab
└──────────────────────────────────────┘
```

### Data Fields — Exercise Card

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | UUID |
| `name` | `string` | `R` | |
| `exerciseType` | `"GYM" \| "SPORT" \| "CARDIO"` | `R` | Badge color: GYM=xanh, SPORT=cam, CARDIO=đỏ |
| `category` | `string \| null` | `O` | Ví dụ: `"Ngực"`, `"Cardio"` |
| `muscleGroup` | `string \| null` | `O` | Nhóm cơ chính |
| `difficultyLevel` | `"BEGINNER" \| "INTERMEDIATE" \| "ADVANCED"` | `R` | |
| `favoritesCount` | `number` | `R` | Hiển thị `♥ 120` |
| `isFavorite` | `boolean` | `R` | Icon tim: filled/outline |
| `imageAvtUrl` | `string \| null` | `O` | Thumbnail; fallback: placeholder |
| `estimatedCaloriesPerMinute` | `number \| null` | `O` | Hiển thị `~7 kcal/phút` |
| `metValue` | `number` | `O` | Dùng khi > 0 thay cho estimatedCalories |

### Filter State

| Field | Kiểu | Default | Ghi chú |
|---|---|---|---|
| `searchQuery` | `string` | `""` | Debounce 300ms |
| `selectedType` | `string \| null` | `null` | `GYM \| SPORT \| CARDIO \| null` |
| `selectedCategory` | `string \| null` | `null` | |
| `selectedDifficulty` | `string \| null` | `null` | |
| `page` | `number` | `1` | Pagination |
| `limit` | `number` | `20` | Items/page |
| `tab` | `"all" \| "favorites"` | `"all"` | |

### API Calls

```
GET /exercises?name=&type=&category=&difficulty=&page=1&limit=20
GET /exercises/popular?limit=10
GET /favorite-exercises                           → tab yêu thích
GET /favorite-exercises/check/:exerciseId         → check từng item
POST /favorite-exercises/:exerciseId              → thêm yêu thích
DELETE /favorite-exercises/:exerciseId            → bỏ yêu thích
```

### Actions
- Tap bài tập → navigate to `strength_detail` / `sport_detail` / `cardio_detail` theo `exerciseType`
- Tap ♥ → toggle favorite (optimistic update)
- Scroll to bottom → load next page (infinite scroll)
- Tab "Yêu thích" → switch to favorites list

---

## 3. strength_detail — Bài tập GYM

**Mục đích:** Màn hình chi tiết bài tập thể lực (sets/reps/weight). Đọc thông tin đầy đủ, xem hướng dẫn, thêm vào session.

### Layout

```
┌──────────────────────────────────────┐
│  [← Back]               [♥ Yêu thích]│
│  ┌────────────────────────────────┐  │
│  │         [VIDEO / ẢNH]          │  │  ← media carousel
│  └────────────────────────────────┘  │
│  Bench Press                         │  ← tên bài tập
│  GYM · Ngực · ⭐ Trung bình         │  ← type + category + difficulty
│  ♥ 120 người yêu thích              │
├──────────────────────────────────────┤
│  Thông số mặc định                   │
│  ┌───────┐ ┌───────┐ ┌───────────┐  │
│  │  4    │ │  10   │ │  60 kg    │  │  ← sets / reps / weight
│  │ Sets  │ │ Reps  │ │  Tạ       │  │
│  └───────┘ └───────┘ └───────────┘  │
│  Nghỉ giữa set: 90 giây             │
├──────────────────────────────────────┤
│  Nhóm cơ mục tiêu                    │
│  [Ngực] [Vai trước] [Cánh tay sau]  │  ← target + secondary muscle chips
├──────────────────────────────────────┤
│  Dụng cụ: Tạ đòn, Ghế băng          │
├──────────────────────────────────────┤
│  Mô tả                               │
│  Lorem ipsum...                       │
├──────────────────────────────────────┤
│  Hướng dẫn thực hiện                 │
│  1. Nằm ngửa trên ghế...             │
│  2. Cầm tạ rộng hơn vai...           │
├──────────────────────────────────────┤
│  Mẹo kỹ thuật                        │
│  💡 Giữ lưng cong nhẹ...            │
├──────────────────────────────────────┤
│  [ + Thêm vào buổi tập hôm nay ]    │  ← CTA
└──────────────────────────────────────┘
```

### Data Fields

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | UUID |
| `name` | `string` | `R` | |
| `exerciseType` | `"GYM"` | `R` | Badge |
| `category` | `string \| null` | `O` | |
| `muscleGroup` | `string \| null` | `O` | Nhóm cơ chính (alias `primaryMuscleGroup`) |
| `targetMuscleGroup` | `string \| null` | `O` | Nhóm cơ đích cụ thể |
| `secondaryMuscleGroups` | `string[]` | `O` | Mảng chip tags |
| `difficultyLevel` | `"BEGINNER" \| "INTERMEDIATE" \| "ADVANCED"` | `R` | |
| `equipment` | `string \| null` | `O` | Fallback: `"Không cần dụng cụ"` |
| `defaultSets` | `number \| null` | `O` | Fallback: `3` |
| `defaultReps` | `number \| null` | `O` | Fallback: `10` |
| `defaultWeightKg` | `number \| null` | `O` | Fallback: `0` |
| `restTimeSeconds` | `number \| null` | `O` | Hiển thị `"90 giây"` |
| `description` | `string \| null` | `O` | |
| `instructions` | `string \| null` | `O` | Parse thành numbered list nếu có `\n` |
| `formTips` | `string \| null` | `O` | |
| `videoUrl` | `string \| null` | `O` | Ưu tiên video trước ảnh |
| `imageAvtUrl` | `string \| null` | `O` | Avatar thumbnail |
| `imageUrl` | `string[]` | `O` | Gallery carousel |
| `favoritesCount` | `number` | `R` | |
| `isFavorite` | `boolean` | `R` | |
| `metValue` | `number` | `O` | Hiển thị nếu > 0: `"MET: 5.0"` |

### Add-to-Session Sheet (Bottom Sheet)

Khi tap **[+ Thêm vào buổi tập]**, mở bottom sheet:

| Input Field | Kiểu | Required | Default | Validation |
|---|---|---|---|---|
| `sets` | `number` | R | `defaultSets ?? 3` | min 1, max 20 |
| `reps` | `number` | R | `defaultReps ?? 10` | min 1, max 100 |
| `weightKg` | `number` | O | `defaultWeightKg ?? 0` | min 0, max 500 |
| `restTimeSeconds` | `number` | O | `restTimeSeconds ?? 90` | min 0, max 600 |
| `durationMinutes` | `number` | O | `null` (auto-compute) | min 1 |
| `note` | `string` | O | `""` | max 500 chars |
| `intensityLevel` | `"LOW"\|"MEDIUM"\|"HIGH"` | O | `"MEDIUM"` | Ảnh hưởng calorie calc |

> Nếu `durationMinutes` bỏ trống → backend tự tính từ `sets × reps × 3s + (sets-1) × restTimeSeconds`

### API Calls

```
GET /exercises/:id                              → load detail
GET /favorite-exercises/check/:id              → isFavorite
POST /favorite-exercises/:id                   → add favorite
DELETE /favorite-exercises/:id                 → remove favorite
POST /training-sessions/:sessionId/items       → add item to session
```

---

## 4. sport_detail — Bài tập SPORT

**Mục đích:** Màn hình chi tiết bài tập vận động (theo thời gian và cường độ). Ví dụ: bóng đá, bơi lội, yoga.

### Layout

```
┌──────────────────────────────────────┐
│  [← Back]               [♥ Yêu thích]│
│  ┌────────────────────────────────┐  │
│  │         [VIDEO / ẢNH]          │  │
│  └────────────────────────────────┘  │
│  Swimming                            │
│  SPORT · Thể thao nước · ⭐ Dễ      │
│  ♥ 87 người yêu thích               │
├──────────────────────────────────────┤
│  Ước tính tiêu hao calo              │
│  ┌──────────┐ ┌──────────┐          │
│  │  4 kcal  │ │  7 kcal  │  10 kcal │  ← LOW / MEDIUM / HIGH
│  │  /phút   │ │  /phút   │  /phút   │
│  └──────────┘ └──────────┘          │
├──────────────────────────────────────┤
│  Thời lượng khuyến nghị: 30 phút    │
│  Loại vận động: Toàn thân           │
│  Cường độ mặc định: MEDIUM          │
├──────────────────────────────────────┤
│  Mô tả · Hướng dẫn · Mẹo kỹ thuật  │
│  (collapsed accordion)               │
├──────────────────────────────────────┤
│  [ + Thêm vào buổi tập hôm nay ]    │
└──────────────────────────────────────┘
```

### Data Fields

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | |
| `name` | `string` | `R` | |
| `exerciseType` | `"SPORT"` | `R` | |
| `category` | `string \| null` | `O` | |
| `muscleGroup` | `string \| null` | `O` | |
| `difficultyLevel` | `string` | `R` | |
| `defaultDurationMinutes` | `number \| null` | `O` | Hiển thị khuyến nghị |
| `defaultIntensityLevel` | `"LOW"\|"MEDIUM"\|"HIGH"\|null` | `O` | Pre-select ở sheet |
| `movementType` | `string \| null` | `O` | Ví dụ: `"Toàn thân"`, `"Chân"` |
| `estimatedCaloriesPerMinute` | `number \| null` | `O` | Hiển thị 3 mức dựa trên intensity constant |
| `metValue` | `number` | `O` | Dùng để hiển thị nếu > 0 |
| `description` | `string \| null` | `O` | |
| `instructions` | `string \| null` | `O` | |
| `formTips` | `string \| null` | `O` | |
| `videoUrl` | `string \| null` | `O` | |
| `imageAvtUrl` | `string \| null` | `O` | |
| `imageUrl` | `string[]` | `O` | |
| `favoritesCount` | `number` | `R` | |
| `isFavorite` | `boolean` | `R` | |

**Calorie preview logic (client-side):**
```
LOW    = estimatedCaloriesPerMinute ?? 4  kcal/phút
MEDIUM = estimatedCaloriesPerMinute ?? 7  kcal/phút
HIGH   = estimatedCaloriesPerMinute ?? 10 kcal/phút
```

### Add-to-Session Sheet

| Input Field | Kiểu | Required | Default | Validation |
|---|---|---|---|---|
| `durationMinutes` | `number` | R | `defaultDurationMinutes ?? 30` | min 1, max 600 |
| `intensityLevel` | `"LOW"\|"MEDIUM"\|"HIGH"` | R | `defaultIntensityLevel ?? "MEDIUM"` | |
| `distanceKm` | `number` | O | `null` | min 0.1, max 1000 |
| `note` | `string` | O | `""` | max 500 |

**Preview calories (live):**
```
caloriesEstimate = intensityRate[selectedIntensity] × durationMinutes
```
Hiển thị realtime trong sheet: `"Ước tính: ~210 kcal"`

---

## 5. cardio_detail — Bài tập CARDIO

**Mục đích:** Bài tập di chuyển (chạy bộ, đạp xe, leo núi). Tính pace và calories tự động từ distance + avgSpeed. Backend tự tính duration và calories.

### Layout

```
┌──────────────────────────────────────┐
│  [← Back]               [♥ Yêu thích]│
│  ┌────────────────────────────────┐  │
│  │         [VIDEO / ẢNH]          │  │
│  └────────────────────────────────┘  │
│  Running                             │
│  CARDIO · Toàn thân · ⭐ Dễ         │
├──────────────────────────────────────┤
│  Mô phỏng nhanh                      │
│  Khoảng cách: [  5.0  ] km           │
│  Tốc độ TB:  [ 10.0  ] km/h          │
│  ─────────────────────────────────   │
│  Thời gian:    30:00 phút  (C)       │
│  Pace:         6:00 /km    (C)       │
│  Ước tính:    ~246 kcal   (C)       │
├──────────────────────────────────────┤
│  Bảng MET theo tốc độ               │
│  < 6 km/h  → Đi bộ nhẹ  (MET 3.5)  │
│  6–8 km/h  → Chạy bộ    (MET 6.0)  │
│  8–10 km/h → Chạy vừa   (MET 8.3)  │
│  > 10 km/h → Chạy nhanh (MET 9.8+) │
├──────────────────────────────────────┤
│  Mô tả · Hướng dẫn                   │
├──────────────────────────────────────┤
│  [ + Thêm vào buổi tập hôm nay ]    │
└──────────────────────────────────────┘
```

### Data Fields — Exercise Info

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | |
| `name` | `string` | `R` | |
| `exerciseType` | `"CARDIO"` | `R` | |
| `category` | `string \| null` | `O` | `"Chạy bộ"`, `"Đạp xe"`, `"Leo núi"` |
| `movementType` | `string \| null` | `O` | Phân biệt cycling vs running cho MET lookup |
| `metValue` | `number` | `O` | Nếu > 0 → dùng để tính thay MET lookup |
| `description` | `string \| null` | `O` | |
| `instructions` | `string \| null` | `O` | |
| `videoUrl` | `string \| null` | `O` | |
| `imageAvtUrl` | `string \| null` | `O` | |
| `favoritesCount` | `number` | `R` | |
| `isFavorite` | `boolean` | `R` | |

### Data Fields — Simulator (client-side computed)

| Field | Kiểu | Nguồn | Công thức |
|---|---|---|---|
| `distanceKm` | `number` | Input | User nhập, min 0.1 |
| `avgSpeedKmh` | `number` | Input | User nhập, min 0.5 |
| `durationMinutes` | `number` | `C` | `(distanceKm / avgSpeedKmh) × 60` |
| `durationDisplay` | `string` | `C` | Format `"MM:SS phút"` |
| `pace` | `string` | `C` | `60 / avgSpeedKmh` → format `"M:SS /km"` |
| `caloriesEstimate` | `number` | `C` | `MET × userWeightKg × (distanceKm / avgSpeedKmh)` |
| `userWeightKg` | `number` | `C` | Lấy từ latestBodyMetric (local cache), fallback 70 |

**MET Lookup (client mirrors backend):**
```
Cycling: < 16 km/h → 4.0 | 16–22 → 8.0 | 22–26 → 10.0 | > 26 → 12.0
Running: < 6 km/h  → 3.5 | 6–8  → 6.0  | 8–10  → 8.3  | 10–12 → 9.8
         12–14     → 11.0 | > 14 → 12.8
```

> Nếu exercise có `metValue > 0` → dùng `metValue` thay lookup table

### Add-to-Session Sheet

| Input Field | Kiểu | Required | Default | Validation |
|---|---|---|---|---|
| `distanceKm` | `number` | R | `5.0` | min 0.1, max 1000 |
| `avgSpeedKmh` | `number` | R | `10.0` | min 0.5, max 80 |
| `pace` | `string` | `C` | auto-computed | Format `M:SS` — chỉ display |
| `durationMinutes` | `number` | `C` | auto-computed | Chỉ display |
| `note` | `string` | O | `""` | max 500 |

> Backend nhận `distanceKm` + `avgSpeedKmh` → tự tính `durationMinutes` + `caloriesBurned`  
> Client chỉ cần gửi: `{ exerciseId, exerciseType: "CARDIO", distanceKm, avgSpeedKmh, pace, note }`

### Pace Formatting Rules

```
pace (min/km) = 60 / avgSpeedKmh

10 km/h → 6.0 min/km → display "6:00 /km"
11 km/h → 5.45 min/km → display "5:27 /km"
8.5 km/h → 7.06 min/km → display "7:04 /km"

Công thức:
  minutes = Math.floor(60 / avgSpeedKmh)
  seconds = Math.round((60 / avgSpeedKmh - minutes) * 60)
  display = `${minutes}:${seconds.toString().padStart(2, '0')} /km`
```

---

## 6. session_workout

**Mục đích:** Màn hình quản lý buổi tập cá nhân — tạo mới, xem chi tiết, thêm/sửa/xóa bài tập trong session, xem tổng kết.

> Màn hình này có **2 mode**: `CREATE` (tạo mới) và `VIEW/EDIT` (xem/chỉnh sửa session đã có).

### Layout — Header & Summary

```
┌──────────────────────────────────────┐
│  [← Back]    Buổi tập 29/05  [⋯ ...]│
│  "Push Day"  (tap để sửa tên)        │
│  ┌─────────┐  ┌─────────┐           │
│  │ 420 kcal│  │ 75 phút │           │  ← totals (real-time update)
│  │ Đã đốt  │  │ Thời gian│           │
│  └─────────┘  └─────────┘           │
├──────────────────────────────────────┤
│  Danh sách bài tập                   │
│  ┌──────────────────────────────┐   │
│  │ 1. Bench Press  (GYM)        │   │
│  │    4 sets × 10 reps @ 60 kg  │   │
│  │    ~20 phút · 148 kcal  [⋯]  │   │
│  ├──────────────────────────────┤   │
│  │ 2. Running      (CARDIO)     │   │
│  │    5 km @ 10 km/h · pace 6:00│   │
│  │    30 phút · 246 kcal   [⋯]  │   │
│  ├──────────────────────────────┤   │
│  │ 3. Swimming     (SPORT)      │   │
│  │    30 phút · MEDIUM          │   │
│  │    25 phút · 210 kcal   [⋯]  │   │
│  └──────────────────────────────┘   │
│  [  + Thêm bài tập  ]               │
├──────────────────────────────────────┤
│  Ghi chú buổi tập: [              ] │
│  [  Lưu buổi tập  ]                 │
└──────────────────────────────────────┘
```

### Data Fields — Session

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string \| null` | `R` | null khi chưa tạo |
| `sessionDate` | `string` | `R` | Format `YYYY-MM-DD`, default today |
| `title` | `string \| null` | `O` | Placeholder: `"Đặt tên buổi tập"` |
| `note` | `string \| null` | `O` | |
| `totalDurationMinutes` | `number` | `C` | Sum từ items |
| `totalCaloriesBurned` | `number` | `C` | Sum từ items |
| `createdAt` | `Date` | `R` | |
| `updatedAt` | `Date` | `R` | |

### Data Fields — Session Item (GYM)

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | |
| `orderIndex` | `number` | `R` | Drag-to-reorder |
| `exercise.id` | `string` | `R` | |
| `exercise.name` | `string` | `R` | |
| `exercise.exerciseType` | `"GYM"` | `R` | |
| `exercise.imageAvtUrl` | `string \| null` | `O` | Thumbnail |
| `exerciseType` | `"GYM"` | `R` | |
| `sets` | `number` | `R` | |
| `reps` | `number` | `R` | |
| `weightKg` | `number \| null` | `O` | Hiển thị `"60 kg"` hoặc `"Bodyweight"` |
| `restTimeSeconds` | `number \| null` | `O` | Hiển thị `"90s nghỉ"` |
| `durationMinutes` | `number` | `C` | Auto-computed từ backend |
| `caloriesBurned` | `number` | `C` | Tính backend |
| `note` | `string \| null` | `O` | |

### Data Fields — Session Item (SPORT)

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | |
| `orderIndex` | `number` | `R` | |
| `exercise.name` | `string` | `R` | |
| `exerciseType` | `"SPORT"` | `R` | |
| `durationMinutes` | `number` | `R` | User nhập |
| `intensityLevel` | `"LOW"\|"MEDIUM"\|"HIGH"` | `R` | |
| `distanceKm` | `number \| null` | `O` | |
| `caloriesBurned` | `number` | `C` | Backend tính |
| `note` | `string \| null` | `O` | |

### Data Fields — Session Item (CARDIO)

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | |
| `orderIndex` | `number` | `R` | |
| `exercise.name` | `string` | `R` | |
| `exerciseType` | `"CARDIO"` | `R` | |
| `distanceKm` | `number` | `R` | |
| `avgSpeedKmh` | `number` | `R` | |
| `pace` | `string \| null` | `O` | Format `"6:00 /km"` |
| `durationMinutes` | `number` | `C` | Backend tính = distance/speed×60 |
| `caloriesBurned` | `number` | `C` | Backend tính |
| `note` | `string \| null` | `O` | |

### API Calls — Flow

**Tạo mới session:**
```
POST /training-sessions
Body: {
  sessionDate: "2026-05-29",
  title: "Push Day",
  note: "...",
  items: [
    { exerciseId, exerciseType: "GYM", sets, reps, weightKg, restTimeSeconds },
    { exerciseId, exerciseType: "CARDIO", distanceKm, avgSpeedKmh, pace }
  ]
}
```

**Xem session có sẵn:**
```
GET /training-sessions/:id
GET /training-sessions/date/:date   → kiểm tra ngày đã có session chưa
```

**Thêm/sửa/xóa item:**
```
POST   /training-sessions/:id/items
PATCH  /training-sessions/:id/items/:itemId
DELETE /training-sessions/:id/items/:itemId
```

**Cập nhật session meta:**
```
PATCH /training-sessions/:id   Body: { title?, note?, sessionDate? }
```

**Xóa session:**
```
DELETE /training-sessions/:id
```

### States

| State | Mô tả |
|---|---|
| `EMPTY_TODAY` | Ngày chưa có session → hiển thị form tạo mới |
| `EXISTING_SESSION` | Đã có session → load và hiển thị items |
| `CONFLICT_409` | Đã có session ngày này khi tạo → đề xuất mở session cũ |
| `LOADING` | Skeleton loading cho item list |
| `SAVING` | Disable CTA, hiển thị spinner |

### Actions
- Drag item → reorder (PATCH `orderIndex`)
- Swipe left trên item → xóa item (với xác nhận)
- Tap item → mở bottom sheet edit tương ứng theo `exerciseType`
- Tap `[+ Thêm bài tập]` → navigate to `exercise_library` với mode "pick" 
- Tap `[Lưu]` → POST nếu chưa có id, PATCH nếu đã có

---

## 7. body_metric

**Mục đích:** Theo dõi chỉ số cơ thể theo thời gian. Nhập cân nặng/chiều cao/số đo, xem lịch sử biểu đồ, upload ảnh tiến trình.

### Layout

```
┌──────────────────────────────────────┐
│  Chỉ số cơ thể          [+ Cập nhật]│
├──────────────────────────────────────┤
│  Chỉ số mới nhất  ·  29/05/2026     │
│  ┌──────────┬──────────┬──────────┐ │
│  │  70.5 kg │  BMI 22.5│   165 cm │ │
│  │ Cân nặng │  Chỉ số  │ Chiều cao│ │
│  └──────────┴──────────┴──────────┘ │
│  ┌──────────┬──────────┐            │
│  │ BMR 1620 │TDEE 2200 │            │
│  │ kcal/ngày│ kcal/ngày│            │
│  └──────────┴──────────┘            │
├──────────────────────────────────────┤
│  Thay đổi so với lần trước          │
│  ↓ -0.3 kg  (từ 29/04)              │
│  ↓ -1.2 kg  (trong 30 ngày)         │
├──────────────────────────────────────┤
│  Biểu đồ cân nặng                    │
│  [Tuần] [Tháng] [3T] [6T] [Năm]     │
│  ╭──────────────────────────╮        │
│  │  71 ┤ ·                  │        │
│  │  70 ┤   · · ·  ·         │        │
│  │     └──────────────────  │        │
│  ╰──────────────────────────╯        │
├──────────────────────────────────────┤
│  Số đo cơ thể (tab phụ)             │
│  Vòng eo: 72cm  Vòng mông: 95cm     │
│  Vòng ngực: 90cm  Bắp tay: 35cm     │
│  Vòng cổ: 35cm                       │
├──────────────────────────────────────┤
│  Ảnh tiến trình                      │
│  [📷 Trước] [📷 Sau] [📷 Bên]        │
│  [+ Upload ảnh mới]                  │
└──────────────────────────────────────┘
```

### Data Fields — Latest Metric Display

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string` | `R` | |
| `measuredAt` | `string` | `R` | Format `"DD/MM/YYYY"` |
| `weightKg` | `number \| null` | `O` | Hiển thị `"--"` nếu null |
| `heightCm` | `number \| null` | `O` | |
| `bmi` | `number \| null` | `C` | Backend tính. Màu: <18.5 xanh nhạt, 18.5–25 xanh, 25–30 vàng, >30 đỏ |
| `bmr` | `number \| null` | `C` | Kcal/ngày cơ bản |
| `tdee` | `number \| null` | `C` | Kcal/ngày tổng (bao gồm hoạt động) |
| `bodyFatPercent` | `number \| null` | `O` | Hiển thị `"18.5%"` |
| `waistCm` | `number \| null` | `O` | |
| `hipCm` | `number \| null` | `O` | |
| `chestCm` | `number \| null` | `O` | |
| `neckCm` | `number \| null` | `O` | |
| `armCm` | `number \| null` | `O` | |
| `notes` | `string \| null` | `O` | |

### Data Fields — Progress Summary

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `startWeight` | `number` | `C` | Cân nặng lần đầu tiên ghi |
| `currentWeight` | `number` | `C` | Cân nặng mới nhất |
| `weightChange` | `number` | `C` | `currentWeight - startWeight` (có dấu) |
| `startDate` | `string` | `C` | Ngày ghi đầu tiên |
| `latestDate` | `string` | `C` | Ngày ghi mới nhất |
| `totalRecords` | `number` | `C` | Tổng số lần đo |

### Data Fields — Chart

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `chartData[].date` | `string` | `R` | x-axis |
| `chartData[].weightKg` | `number \| null` | `R` | y-axis |
| `selectedPeriod` | `"week"\|"month"\|"3months"\|"6months"\|"year"` | State | |

### Data Fields — Progress Photos

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `photos[].id` | `string` | `R` | |
| `photos[].photoUrl` | `string` | `R` | CDN URL |
| `photos[].photoType` | `"front"\|"back"\|"side"` | `R` | Tab grouping |
| `photos[].takenAt` | `Date` | `R` | |
| `photos[].bodyMetricId` | `string \| null` | `O` | Link đến metric record |

### Form — Cập nhật chỉ số (Bottom Sheet / Modal)

**Tab Cơ bản:**
| Input | Kiểu | Required | Validation |
|---|---|---|---|
| `weightKg` | `number` | R | 20–500 |
| `heightCm` | `number` | R | 50–300 |
| `measuredAt` | `date` | O | Default: today |

**Tab Nâng cao:**
| Input | Kiểu | Required | Validation |
|---|---|---|---|
| `bodyFatPercent` | `number` | O | 1–70 |
| `waistCm` | `number` | O | 20–300 |
| `hipCm` | `number` | O | 20–300 |
| `chestCm` | `number` | O | 20–300 |
| `neckCm` | `number` | O | 5–100 |
| `armCm` | `number` | O | 10–100 |
| `notes` | `string` | O | max 500 |

### API Calls

```
GET /body-metrics/latest                → load latest metric
GET /body-metrics/summary               → progress summary
GET /body-metrics/period/:period        → chart data (week/month/...)
POST /body-metrics/basic                → upsert cơ bản
POST /body-metrics/advanced             → upsert nâng cao
GET /body-metrics/photos?limit=20       → load photos
POST /body-metrics/photos               → upload (multipart/form-data)
DELETE /body-metrics/photos/:id         → xóa ảnh
```

### BMI Classification (Hiển thị màu)

| BMI | Label | Màu |
|---|---|---|
| < 18.5 | Thiếu cân | `#60A5FA` (xanh nhạt) |
| 18.5–24.9 | Bình thường | `#34D399` (xanh lá) |
| 25–29.9 | Thừa cân | `#FBBF24` (vàng) |
| ≥ 30 | Béo phì | `#F87171` (đỏ) |

---

## 8. activity_log

**Mục đích:** Nhật ký hoạt động hàng ngày — nước uống, bước chân, giấc ngủ, tâm trạng. Dữ liệu feed vào Dashboard.

### Layout

```
┌──────────────────────────────────────┐
│  Nhật ký hôm nay        [📅 Chọn ngày│
│  Thứ Sáu, 29/05/2026                 │
├──────────────────────────────────────┤
│  💧 Nước uống                         │
│  ████████████░░░░░░  2000/2500ml     │  ← progress bar
│  [-250ml]              [+250ml]      │
│  [-500ml]              [+500ml]      │
├──────────────────────────────────────┤
│  👣 Bước chân                         │
│  ████████░░░░░░░░░░  8500/10000      │  ← progress bar
│  [Nhập tay: _______  bước]           │
├──────────────────────────────────────┤
│  😴 Giấc ngủ                          │
│  [  7.5  ] giờ                        │
│  ───────────────────────────────────  │
│  🎭 Tâm trạng                         │
│  [😊]  [😐]  [😔]  [😤]  [💪]        │
├──────────────────────────────────────┤
│  📝 Ghi chú                           │
│  [____________________________]       │
├──────────────────────────────────────┤
│  7 ngày gần đây                       │
│  T2  T3  T4  T5  T6  T7  CN         │
│  💧  💧  💧  --  💧  💧  --          │  ← streak dots
│  👣  👣  --  👣  👣  --  👣          │
└──────────────────────────────────────┘
```

### Data Fields — Daily Log

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `id` | `string \| null` | `R` | null nếu chưa có log ngày này |
| `logDate` | `string` | `R` | Format `YYYY-MM-DD` |
| `waterMl` | `number` | `R` | Default `0`, max `10000` |
| `steps` | `number` | `R` | Default `0`, max `100000` |
| `sleepHours` | `number \| null` | `O` | Decimal, ví dụ `7.5` |
| `mood` | `string \| null` | `O` | Enum: `happy \| neutral \| sad \| angry \| energetic` |
| `note` | `string \| null` | `O` | max 500 chars |
| `updatedAt` | `Date` | `R` | |

### Data Fields — Goals (from User Health Profile)

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `waterGoalMl` | `number` | Profile | Default `2000`, dùng cho progress bar |
| `stepGoal` | `number` | Config | Default `10000` |

### Data Fields — Weekly Overview (7 ngày)

| Field | Kiểu | Nguồn | Ghi chú |
|---|---|---|---|
| `weeklyLogs[].logDate` | `string` | `R` | |
| `weeklyLogs[].waterMl` | `number` | `R` | |
| `weeklyLogs[].steps` | `number` | `R` | |
| `weeklyLogs[].hasLog` | `boolean` | `C` | `waterMl > 0 || steps > 0` |
| `weeklyLogs[].reachedWaterGoal` | `boolean` | `C` | `waterMl >= waterGoalMl` |
| `weeklyLogs[].reachedStepGoal` | `boolean` | `C` | `steps >= stepGoal` |

### Mood Options

| Value | Emoji | Label |
|---|---|---|
| `happy` | 😊 | Vui vẻ |
| `neutral` | 😐 | Bình thường |
| `sad` | 😔 | Buồn |
| `angry` | 😤 | Căng thẳng |
| `energetic` | 💪 | Tràn đầy năng lượng |

### API Calls

```
GET /activity-logs?date=YYYY-MM-DD         → load log ngày chỉ định (empty nếu chưa có)
GET /activity-logs/range?fromDate=&toDate= → 7 ngày weekly overview

PATCH /activity-logs/water   { waterMl: number }
PATCH /activity-logs/steps   { steps: number }
PATCH /activity-logs/sleep   { sleepHours: number }
PATCH /activity-logs/mood    { mood: string }
PATCH /activity-logs/note    { note: string }
```

> **Lưu ý:** Tất cả 5 PATCH endpoints là **upsert** — không cần tạo record trước. Mỗi field update độc lập để tránh race condition khi user thao tác nhanh.

### Quick-add Water Buttons

| Button | Delta |
|---|---|
| `+250ml` | +250 |
| `+500ml` | +500 |
| `-250ml` | -250 (min 0) |
| `-500ml` | -500 (min 0) |

Mỗi tap → PATCH ngay (debounce 500ms nếu tap liên tục). Hiển thị optimistic update trước khi response về.

### Notification Logic (client + server)

- Khi `waterMl` vượt `waterGoalMl` lần đầu trong ngày → Server gửi push notification `"🎉 Bạn đã đạt mục tiêu uống nước hôm nay!"`
- Client hiển thị confetti animation khi progress bar đạt 100%

### Empty State
- Ngày chưa có log: hiển thị `waterMl = 0`, `steps = 0`, tất cả form trống
- API trả về log rỗng (default values), không phải 404

---

## Phụ lục — Mapping API → Screen

| Screen | Endpoints sử dụng |
|---|---|
| workout_home | `GET /training-sessions` (range + limit) |
| exercise_library | `GET /exercises`, `GET /exercises/popular`, `GET /favorite-exercises` |
| strength_detail | `GET /exercises/:id`, `POST /favorite-exercises/:id`, `DELETE /favorite-exercises/:id`, `POST /training-sessions/:id/items` |
| sport_detail | (same as strength_detail) |
| cardio_detail | (same as strength_detail) |
| session_workout | `GET /training-sessions/:id`, `POST /training-sessions`, `PATCH /training-sessions/:id`, `DELETE /training-sessions/:id`, `POST /training-sessions/:id/items`, `PATCH /training-sessions/:id/items/:itemId`, `DELETE /training-sessions/:id/items/:itemId` |
| body_metric | `GET /body-metrics/latest`, `GET /body-metrics/summary`, `GET /body-metrics/period/:p`, `POST /body-metrics/basic`, `POST /body-metrics/advanced`, `GET /body-metrics/photos`, `POST /body-metrics/photos`, `DELETE /body-metrics/photos/:id` |
| activity_log | `GET /activity-logs`, `GET /activity-logs/range`, `PATCH /activity-logs/water`, `/steps`, `/sleep`, `/mood`, `/note` |

## Phụ lục — Calorie Calculation Summary (Client Preview)

| Type | Formula (client preview) | Source of truth |
|---|---|---|
| GYM | `GYM_FACTOR[intensity] × userWeight × duration / 60` | Backend |
| SPORT | `estimatedCaloriesPerMinute ?? INTENSITY_RATE[intensity] × duration` | Backend |
| CARDIO | `MET[speed] × userWeight × (distance / speed)` | Backend |

> Client chỉ dùng formula để **preview** trong real-time. Giá trị **chính thức** luôn là `caloriesBurned` do backend trả về sau khi lưu item.
