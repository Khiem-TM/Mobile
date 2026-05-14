# Food Classifier Android App

Ứng dụng Android nhận diện món ăn Việt Nam theo thời gian thực sử dụng camera và mô hình TFLite dựa trên EfficientNetV2.

---

## Mục lục

- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cấu trúc dự án](#cấu-trúc-dự-án)
- [Mô hình & nhãn](#mô-hình--nhãn)
- [Hướng dẫn chạy](#hướng-dẫn-chạy)
- [Chi tiết kỹ thuật](#chi-tiết-kỹ-thuật)
- [Thêm mô hình mới](#thêm-mô-hình-mới)
- [Xử lý sự cố](#xử-lý-sự-cố)

---

## Yêu cầu hệ thống

| Thành phần | Yêu cầu |
|---|---|
| Android Studio | Hedgehog (2023.1.1) trở lên |
| Android SDK | API 23 (Android 6.0) trở lên |
| Target SDK | API 34 (Android 14) |
| JDK | 11 trở lên |
| Gradle | 8.x |

---

## Cấu trúc dự án

```
Food-Model/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   ├── food_model_v2_compat.tflite   ← Mô hình đang dùng
│   │   │   ├── labels3.txt                   ← File nhãn tương ứng
│   │   │   └── ...                           (các file khác, không dùng)
│   │   ├── java/.../imageclassification/
│   │   │   ├── ImageClassifierHelper.kt      ← Lõi xử lý TFLite
│   │   │   ├── MainActivity.kt
│   │   │   └── fragments/
│   │   │       ├── CameraFragment.kt         ← Giao diện camera + kết quả
│   │   │       ├── ClassificationResultsAdapter.kt
│   │   │       └── PermissionsFragment.kt
│   │   └── res/layout/
│   │       ├── fragment_camera.xml
│   │       └── item_classification_result.xml
│   └── build.gradle
└── build.gradle
```

---

## Mô hình & nhãn

### Mô hình đang sử dụng

| Thuộc tính | Giá trị |
|---|---|
| File | `food_model_v2_compat.tflite` |
| Architecture | EfficientNetV2 (fine-tuned) |
| Input shape | `(1, 224, 224, 3)` — float32 |
| Input range | **[0, 255]** — KHÔNG normalize thêm |
| Output | Softmax probabilities `(1, 30)` |
| Số lớp | 30 món ăn Việt Nam |

> **Quan trọng:** EfficientNetV2 đã tích hợp sẵn lớp Rescaling bên trong model.
> Truyền thẳng pixel `[0, 255]` dưới dạng `float32`, **không** chia cho 255 hay normalize thêm.
> **Không** áp dụng softmax ở phía app vì model đã có softmax layer ở cuối.

### 30 món ăn được nhận diện

| # | Món ăn | # | Món ăn |
|---|---|---|---|
| 1 | Bánh bèo | 16 | Bún đậu mắm tôm |
| 2 | Bánh bột lọc | 17 | Bún mắm |
| 3 | Bánh căn | 18 | Bún riêu |
| 4 | Bánh canh | 19 | Bún thịt nướng |
| 5 | Bánh chưng | 20 | Cá kho tộ |
| 6 | Bánh cuốn | 21 | Canh chua |
| 7 | Bánh đúc | 22 | Cao lầu |
| 8 | Bánh giò | 23 | Cháo lòng |
| 9 | Bánh khọt | 24 | Cơm tấm |
| 10 | Bánh mì | 25 | Gỏi cuốn |
| 11 | Bánh pía | 26 | Hủ tiếu |
| 12 | Bánh tét | 27 | Mì Quảng |
| 13 | Bánh tráng nướng | 28 | Nem chua |
| 14 | Bánh xèo | 29 | Phở |
| 15 | Bún bò Huế | 30 | Xôi xéo |

---

## Hướng dẫn chạy

### Bước 1 — Mở dự án

Mở Android Studio → **File > Open** → chọn thư mục `Food-Model`.

### Bước 2 — Đồng bộ Gradle

Android Studio sẽ tự động sync. Nếu không, nhấn:
**File > Sync Project with Gradle Files**

### Bước 3 — Chuẩn bị thiết bị

**Thiết bị thật (khuyến nghị để có kết quả tốt nhất):**
1. Vào **Cài đặt > Giới thiệu điện thoại** → nhấn **Số hiệu bản dựng** 7 lần để bật Developer Options
2. Vào **Cài đặt > Tùy chọn nhà phát triển** → bật **Gỡ lỗi USB**
3. Cắm cáp USB vào máy tính, chọn **Truyền file** khi được hỏi
4. Thiết bị sẽ hiện trong dropdown trên Android Studio

**Emulator (chỉ để test giao diện):**
1. **Tools > AVD Manager** → Create Virtual Device
2. Chọn thiết bị bất kỳ, chọn API level ≥ 23
3. Để test nhận diện trên emulator: mở **Extended Controls (...)** → **Camera** → chọn **Webcam0** (dùng webcam máy tính) hoặc chỉ camera emulator vào màn hình đang hiển thị ảnh đồ ăn

### Bước 4 — Chạy ứng dụng

Nhấn **Run ▶** hoặc `Shift+F10`.

Hoặc build thủ công qua terminal:

```bash
# Windows
.\gradlew.bat assembleDebug

# Cài lên thiết bị đang kết nối
adb install app\build\outputs\apk\debug\app-debug.apk
```

### Bước 5 — Cấp quyền camera

Lần đầu mở app, màn hình yêu cầu quyền camera sẽ hiện ra → nhấn **Cho phép**.

### Bước 6 — Sử dụng

- Hướng camera vào món ăn
- Kết quả **top 5 món** kèm độ tin cậy (%) hiển thị ở card phía dưới
- Thời gian inference (ms) hiển thị góc phải card

---

## Chi tiết kỹ thuật

### Pipeline xử lý ảnh

```
Camera frame (RGBA 8888)
        ↓
Bitmap.createScaledBitmap(224 × 224)
        ↓
getPixels() → tách R, G, B từng pixel
        ↓
putFloat(R), putFloat(G), putFloat(B)   ← giá trị [0.0, 255.0]
        ↓
buffer.rewind()          ← BẮT BUỘC, reset position về 0
        ↓
Interpreter.run(buffer, outputArray[1][30])
        ↓
outputArray[0][i] = probability của lớp i   ← tổng = 1.0
        ↓
Sort giảm dần → hiển thị top 5
```

### Code preprocessing (`ImageClassifierHelper.kt`)

```kotlin
private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
    val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
    val buffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
    buffer.order(ByteOrder.nativeOrder())
    val pixels = IntArray(inputWidth * inputHeight)
    scaled.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
    for (pixel in pixels) {
        // EfficientNetV2: truyền thẳng [0, 255], KHÔNG normalize
        buffer.putFloat((pixel shr 16 and 0xFF).toFloat())  // R
        buffer.putFloat((pixel shr 8  and 0xFF).toFloat())  // G
        buffer.putFloat((pixel        and 0xFF).toFloat())  // B
    }
    buffer.rewind()  // ← quan trọng: reset trước khi truyền vào model
    return buffer
}
```

### Dependency TFLite (`app/build.gradle`)

```gradle
// Dùng LiteRT 1.0.1 — hỗ trợ FULLY_CONNECTED v12 mà tensorflow-lite cũ không có
implementation 'com.google.ai.edge.litert:litert:1.0.1'
```

### Luồng điều hướng

```
MainActivity
    └── NavHostFragment (fragment_container)
            ├── PermissionsFragment  →  kiểm tra & yêu cầu quyền camera
            └── CameraFragment
                    ├── CameraX (ImageAnalysis)     →  lấy frame liên tục
                    ├── ImageClassifierHelper       →  chạy TFLite inference
                    └── ClassificationResultsAdapter →  cập nhật RecyclerView
```

---

## Thêm mô hình mới

### 1. Chuẩn bị file

Đặt vào `app/src/main/assets/`:
```
ten_mo_hinh.tflite
labels_moi.txt        ← mỗi dòng một nhãn, không cần số thứ tự
```

Ví dụ `labels_moi.txt`:
```
Tên món 1
Tên món 2
Tên món 3
```

### 2. Cập nhật `ImageClassifierHelper.kt`

```kotlin
companion object {
    const val MODEL_NAME  = "ten_mo_hinh.tflite"   // ← tên file model
    const val LABELS_FILE = "labels_moi.txt"        // ← tên file nhãn
}
```

### 3. Kiểm tra preprocessing theo loại model

| Loại model | Input range | Code cần dùng |
|---|---|---|
| EfficientNetV2 | [0, 255] | `x.toFloat()` — giữ nguyên như hiện tại |
| MobileNetV2 | [-1, 1] | `(x - 127.5f) / 127.5f` |
| Custom với `rescaling 1/255` | [0, 1] | `x / 255.0f` |

### 4. Kiểm tra output

Nếu model **không có softmax layer** (output là logits), thêm softmax trong code:

```kotlin
// Trong hàm classify(), sau khi chạy interpreter.run():
val scores = softmax(outputArray[0])   // thêm dòng này

private fun softmax(logits: FloatArray): FloatArray {
    val max = logits.max()
    val exp = FloatArray(logits.size) { i -> kotlin.math.exp((logits[i] - max).toDouble()).toFloat() }
    val sum = exp.sum()
    return FloatArray(exp.size) { i -> exp[i] / sum }
}
```

---

## Xử lý sự cố

### App crash ngay khi mở

**Lỗi:** `The style on this component requires your app theme to be Theme.MaterialComponents`

**Fix** — kiểm tra `res/values/styles.xml`:
```xml
<style name="AppTheme" parent="Theme.MaterialComponents.Light.NoActionBar">
</style>
```

---

### Model không load được

**Lỗi:** `Didn't find op for builtin opcode 'FULLY_CONNECTED' version '12'`

**Nguyên nhân:** `tensorflow-lite` cũ (< 2.18) không hỗ trợ op này.

**Fix** — `app/build.gradle`:
```gradle
// Xóa dòng cũ nếu có:
// implementation 'org.tensorflow:tensorflow-lite:x.x.x'

// Thay bằng:
implementation 'com.google.ai.edge.litert:litert:1.0.1'
```

---

### Kết quả trống, không hiển thị gì

**Nguyên nhân 1 — thiếu `buffer.rewind()`:**

Model nhận toàn bộ input = 0 vì ByteBuffer đang ở cuối sau khi fill data.
Kiểm tra hàm `preprocessBitmap()` phải có `buffer.rewind()` trước `return`.

**Nguyên nhân 2 — normalize sai:**

Dùng sai normalization so với lúc train model. Xem bảng ở mục [Thêm mô hình mới](#3-kiểm-tra-preprocessing-theo-loại-model).

**Nguyên nhân 3 — áp dụng softmax 2 lần:**

Nếu model đã có softmax layer (output tổng = 1.0), không áp dụng softmax thêm lần nữa trong app.

---

### Scores đều thấp (~3%)

**Nguyên nhân:** Camera không thấy đồ ăn (nền trống, tối, hoặc emulator virtual scene).

**Test nhanh:**
- Hướng camera vào ảnh đồ ăn trên màn hình máy tính
- Hoặc in ảnh đồ ăn rồi chụp

---

### Kiểm tra log realtime

```bash
adb logcat -s "ImageClassifierHelper:D"
```

Output mẫu khi nhận diện tốt (scores phân bố rõ ràng):
```
D/ImageClassifierHelper: Top3: [Pho=82.3%, Com tam=9.1%, Bun bo Hue=3.2%]
```

Output khi có vấn đề (scores đều nhau = model nhận input sai):
```
D/ImageClassifierHelper: Top3: [Banh chung=3.4%, Nem chua=3.4%, Banh tet=3.4%]
```
