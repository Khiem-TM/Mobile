Kế Hoạch Triển Khai Mobile — Kotlin Jetpack Compose
Kiến Trúc Tổng Thể

mobile/
├── app/src/main/
│ ├── data/
│ │ ├── remote/ # Retrofit API interfaces
│ │ ├── local/ # Room DB (offline cache)
│ │ ├── repository/ # Repository pattern
│ │ └── model/ # DTOs + domain models
│ ├── di/ # Hilt modules
│ ├── ui/
│ │ ├── theme/ # Color, Typography, Shape
│ │ ├── components/ # Shared Composables
│ │ └── screens/ # Mỗi screen = 1 package
│ └── navigation/ # NavHost + Routes
Stack cố định:

UI: Jetpack Compose + Material 3
Nav: Navigation Compose (type-safe routes)
DI: Hilt
Network: Retrofit + OkHttp + Moshi
Auth token: DataStore (thay SharedPreferences)
Image: Coil
Camera/Scan: CameraX + ML Kit (barcode)
Charts: Vico (lightweight Compose chart lib)
Offline cache: Room
Phân Chia Màn Hình Theo Module
Module A — Onboarding (không có TabBar)
Screen Route API
ScreenWelcome welcome —
ScreenSignIn signin POST /auth/login, Google OAuth
ScreenOnboard1 onboard/1 — (local state)
ScreenOnboard2 onboard/2 —
ScreenOnboard3 onboard/3 —
ScreenOnboard4 onboard/4 PUT /users/me/health-profile
Lưu ý triển khai:

Dùng BackHandler để xử lý nút back vật lý trong flow onboarding
Lưu data onboarding tạm trong SavedStateHandle (survive process death)
Step 4 gọi API sau khi user xác nhận, không phải từng bước
Module B — Home (TabBar)
Screen Route API
ScreenHome home GET /dashboard?date=, GET /streaks, GET /notifications/unread-count
Components phức tạp:

ArcGaugeCalories — vẽ bằng Canvas Compose, animate với animateFloatAsState
WeekStrip — LazyRow với item ngày, highlight hôm nay
MacroBarRow — 3 LinearProgressIndicator màu (Carbs/Protein/Fat)
WaterTracker — 10 dot indicators, tap để log nước
StreakBanner — gradient card với số ngày streak
Module C — Diary / Food (TabBar + sub-screens)
Screen Route API
ScreenDiary diary GET /meal-logs?date=, GET /meal-logs/summary
ScreenSearchFood diary/search GET /foods?name=, GET /foods/favorites
ScreenFoodDetail diary/food/{id} GET /foods/{id}
ScreenCreateFood diary/food/create POST /foods, POST /meal-logs/{id}/items
Logic đặc biệt:

Diary dùng date state + LazyColumn với section header mỗi bữa (Breakfast/Lunch/Snack/Dinner)
Serving picker trong FoodDetail: var serving by remember { mutableFloatStateOf(1f) } — tính toán realtime
Search debounce 400ms dùng Flow.debounce
Phân tab: All / Recent / My Foods / Favorites / Brands — TabRow + HorizontalPager
Module C′ — AI Scan (FAB center TabBar)
Screen Route API
ScreenScan scan POST /ai-scan/analyze (multipart), GET /foods/barcode/{code}
Triển khai:

CameraX preview trong AndroidView Composable
Hai mode: Camera AI (chụp → upload) và Barcode (ML Kit real-time)
Upload ảnh bằng Multipart Retrofit, hiển thị CircularProgressIndicator
Kết quả: Bottom Sheet với danh sách detected foods + nút "Thêm vào bữa"
Module D — Workout (từ Home navigate vào)
Screen Route API
ScreenWorkout workout GET /training/sessions, GET /activity-logs?date=
ScreenExerciseList workout/exercises GET /training/exercises?muscleGroup=
ScreenWorkoutSession workout/session/{id} POST /training/sessions, POST /training/sessions/{id}/exercises
Components:

Category grid (Cardio/Strength/HIIT/Yoga) — LazyVerticalGrid(columns = Fixed(2))
Session card với duration timer — LaunchedEffect + coroutine delay
Steps/Water log — PATCH endpoints, optimistic update
Module E — Body Metrics (từ Profile navigate vào)
Screen Route API
ScreenMetrics profile/metrics GET /body-metrics/latest, GET /body-metrics/period/{period}, GET /body-metrics/photos
Components:

Weight chart bằng Vico (CartesianChartHost) — timeline 1W/1M/3M
BMI indicator — custom Canvas arc với color zone (underweight/normal/overweight)
Progress photo grid — LazyVerticalGrid + full-screen viewer
Module F — AI Coach (TabBar)
Screen Route API
ScreenCoach coach GET /chatbot/sessions, POST /chatbot/sessions, POST /chatbot/sessions/{id}/messages
Triển khai:

Chat UI: LazyColumn(reverseLayout = true) cho bubble scroll-to-bottom tự động
Session list: ModalBottomSheet chọn lịch sử hội thoại
Streaming response: poll hoặc SSE — dùng Flow emit từng chunk vào bubble
Typing indicator: 3 dot animation với InfiniteTransition
Module G — Discover / Blog (từ Home navigate vào)
Screen Route API
ScreenDiscover discover GET /blogs?tag=&page=
ScreenBlogDetail discover/{id} GET /blogs/{id}
Infinite scroll bằng Paging 3 + collectAsLazyPagingItems()
Tag filter: FlowRow chip group
Module H — Notifications (từ Home bell icon)
Screen Route API
ScreenNotifications notifications GET /notifications, PATCH /notifications/{id}/read
Swipe-to-delete bằng SwipeToDismissBox
Read/unread visual state
Module I — Profile (TabBar)
Screen Route API
ScreenProfile profile GET /users/me, GET /streaks, PATCH /users/{id}
Avatar upload: ActivityResultContracts.PickVisualMedia() + POST /users/me/avatar
Settings section: Dark mode toggle, logout, deactivate account
Phân Chia Phase Triển Khai

Phase 1 — Foundation (3-4 ngày)
├── Project setup: Hilt, Retrofit, Room, Navigation, Theme
├── DataStore cho JWT token management (auto-refresh interceptor)
└── Base composables: AppBar, BottomNavBar, LoadingState, ErrorState

Phase 2 — Auth + Onboarding (2-3 ngày)
├── Welcome → SignIn → Onboard 1-4
└── Google OAuth (Chrome Custom Tabs)

Phase 3 — Home + Dashboard (2 ngày)
├── ArcGauge Canvas component
├── Dashboard API integration
└── WeekStrip + Macro bars

Phase 4 — Diary + Food (4-5 ngày) ← core feature
├── Meal log CRUD
├── Food search + detail
├── Serving size calculator
└── Create custom food

Phase 5 — AI Scan (2-3 ngày)
├── CameraX setup
├── ML Kit barcode
└── Gemini vision upload

Phase 6 — Workout + Metrics (3 ngày)
├── Exercise browser
├── Session builder
└── Body metrics chart (Vico)

Phase 7 — AI Coach + Blog (2 ngày)
└── Chat UI + Chatbot API

Phase 8 — Polish (2 ngày)
├── Animations (shared element transitions)
├── Offline handling (Room cache)
└── Dark mode

Thiết kế các trang và giao diện dựa trên các bản thiết kế bên trong folder: design/Mobile
