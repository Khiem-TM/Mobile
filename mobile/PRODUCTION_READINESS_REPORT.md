# Mobile Production Readiness Report

Ngày kiểm tra: 2026-05-29

## 1. Tổng quan hiện trạng

Folder `mobile` là Android app Kotlin dùng Jetpack Compose. Cấu trúc hiện tại đã có các lớp production quan trọng: `data`, `di`, navigation, reusable UI components, nhiều `ViewModel` theo từng màn, Retrofit/OkHttp, Hilt, Room, DataStore, WorkManager, Coil và CameraX/TFLite cho flow scan.

App đang đi theo MVVM thực dụng: UI Compose observe `StateFlow` từ `ViewModel`, `ViewModel` gọi repository, repository gọi API/local storage. Tuy nhiên dự án chưa có layer `domain`/use case rõ ràng; business logic vẫn còn nằm nhiều trong repository/ViewModel, ví dụ training/manual activity aggregation.

## 2. Techstack đã có

- Kotlin Android, Jetpack Compose, Navigation Compose.
- Hilt DI và `@HiltViewModel`.
- Retrofit + OkHttp + Moshi.
- Token attach interceptor và token refresh `Authenticator`.
- DataStore cho token/session.
- Room database cho meal log cache/pending sync.
- WorkManager + Hilt worker cho pending sync.
- Coil image loading.
- CameraX và LiteRT/TFLite.
- Google Sign-In.

## 3. Phần đã bổ sung/refactor

- Tách `BASE_URL` theo build type:
  - Debug: dùng `BASE_URL` từ `local.properties`/`VITALAI_BASE_URL`, fallback `http://10.0.2.2:3005/`.
  - Release: dùng `BASE_URL` từ config, fallback HTTPS placeholder `https://api.vitalai.invalid/` để tránh HTTP production.
- Bật `BuildConfig.LOG_NETWORK_BODY` chỉ cho debug.
- OkHttp có timeout 30s và redaction cho `Authorization`, `Cookie`, `Set-Cookie`.
- Release bật R8/minify và shrink resources, thêm `proguard-rules.pro` nền tảng.
- Production network security mặc định chặn cleartext; debug overlay cho phép `10.0.2.2`/`localhost`.
- Thêm `AppError`/`AppErrorMapper` để chuẩn hóa lỗi network/unauthorized/server/validation/unknown.
- Thêm `AppLogger` wrapper, chỉ log debug và mask thông tin nhạy cảm.
- Thêm `AppSettingsDataStore` cho onboarding/theme/language/pending FCM token.
- Thêm Firebase Messaging dependency, manifest service và `VitalFirebaseMessagingService`.
- Thêm `DeviceTokenRegistrar` lưu FCM token local chờ backend endpoint.
- Thêm CrashReporter skeleton được init từ `VitalAIApp`.
- Thêm test dependencies và unit test mẫu cho `AppErrorMapper`.
- Sửa lint locale trong `ProfileScreen`.

## 4. Phần còn thiếu hoặc cần cấu hình ngoài

- Firebase:
  - Cần thêm `google-services.json`.
  - Cần cấu hình Firebase project/dashboard.
  - Nên thêm Google Services/Crashlytics Gradle plugins khi có file cấu hình.
- Push notification:
  - Backend chưa thấy API contract để đăng ký device token.
  - Cần endpoint gửi/xóa token theo user/session.
  - Cần notification channel và mapping payload chi tiết.
- Crash reporting:
  - Đã có skeleton, chưa forward exception thật sang Crashlytics vì thiếu Firebase config.
- Domain/Clean Architecture:
  - Chưa có `domain` package/use case chính thức.
  - Nên migrate dần các logic phức tạp từ repository/ViewModel sang use case.
- Pagination:
  - API food có page/limit, nhưng UI chưa dùng Paging 3.
  - Nên áp dụng Paging 3 cho food/history/blog/notifications nếu data lớn.
- Token storage:
  - Token đang dùng DataStore thường. Tốt hơn SharedPreferences thường, nhưng chưa mã hóa.
  - Nên cân nhắc Encrypted DataStore/EncryptedSharedPreferences nếu threat model yêu cầu.
- Release:
  - Chưa có signing config production.
  - `release BASE_URL` cần cấu hình bằng `local.properties` hoặc env `VITALAI_BASE_URL`.
- Version management:
  - Có `libs.versions.toml` nhưng app module vẫn hardcode nhiều dependency.
  - Nên migrate dependency sang version catalog đồng bộ.

## 5. Checklist production

| Hạng mục | Trạng thái | Ghi chú |
| --- | --- | --- |
| Architecture | Partial | MVVM rõ, thiếu domain/use case layer chính thức. |
| UI | OK | Compose + Navigation Compose, UI chủ yếu observe state từ ViewModel. |
| State management | OK | ViewModel dùng `StateFlow`; một số màn còn local UI state hợp lý. |
| Networking | OK | Retrofit/OkHttp/Moshi, auth interceptor, refresh authenticator, timeout, debug-only body logging. |
| Auth | Partial | Token/session đầy đủ, logout clear session; token chưa mã hóa. |
| Local cache | Partial | Room có meal log/pending sync; chưa cache toàn bộ profile/food/training. |
| Offline support | Partial | WorkManager pending sync đã có cho một số action. |
| DI | OK | Hilt app/module/ViewModel/worker đã có. |
| Background work | OK | WorkManager foundation đã có. |
| Pagination | Partial | Backend call có page/limit; chưa có Paging 3. |
| Image loading | OK | Coil/AsyncImage/SubcomposeAsyncImage đã dùng. |
| Push notification | Skeleton | Đã thêm FCM service/token registrar; cần Firebase config và backend endpoint. |
| Crash reporting | Skeleton | Đã thêm CrashReporter skeleton; cần Firebase config để bật thật. |
| Security | Improved | Release chặn cleartext, debug overlay riêng, logging redaction, R8 bật. |
| Error handling | Improved | Có `AppErrorMapper`; cần migrate dần repository còn lại. |
| Logging | Improved | Có `AppLogger`, OkHttp release không log body. |
| Build release | Partial | R8 bật, thiếu signing config và real release `BASE_URL`. |
| Testing | Partial | Có unit test mẫu; cần mở rộng ViewModel/repository/API/UI test. |
| Monitoring/logging | Partial | Logger + CrashReporter skeleton; monitoring thật chưa bật. |

## 6. Kết quả kiểm tra build/test

Đã chạy thành công:

```bash
./gradlew testDebugUnitTest assembleDebug
./gradlew lintDebug
```

Kết quả: build, unit test và lint đều pass.

Các warning còn lại:

- Moshi KAPT deprecated: nên migrate sang KSP.
- Room warning: `MealLogItemEntity.mealLogId` foreign key nên có index.
- GoogleSignIn deprecated: nên migrate sang Credential Manager/Google Identity Services.
- Gradle/dependency version warnings: nên nâng theo batch có regression test.
- Gradle deprecated features: cần kiểm tra bằng `--warning-mode all` trước khi lên Gradle 9.

## 7. Ưu tiên phát triển tiếp theo

1. Cấu hình release thật: signing config, real HTTPS `BASE_URL`, Firebase project, `google-services.json`.
2. Bổ sung backend endpoint đăng ký/xóa FCM token và sync token đã lưu trong `AppSettingsDataStore`.
3. Migrate các repository sang error handling chung bằng `AppErrorMapper`/sealed result.
4. Tách `domain` package và thêm use case cho auth/session, meal log, training aggregation, sync.
5. Mở rộng Room cache cho user profile, food list, training/history cần offline.
6. Áp dụng Paging 3 cho danh sách lớn.
7. Migrate Moshi KAPT sang KSP và dependency sang version catalog.
8. Thêm test có ý nghĩa: ViewModel state, repository với MockWebServer, Compose UI smoke test.
9. Xử lý Room index warning và GoogleSignIn deprecation.
10. Bật Crashlytics thật, non-fatal reporting và release monitoring sau khi có Firebase config.
