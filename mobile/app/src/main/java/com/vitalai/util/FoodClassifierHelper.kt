package com.vitalai.util

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class ClassificationResult(val label: String, val confidence: Float)

private val labelToVietnamese = mapOf(
    "Banh beo" to "Bánh bèo",
    "Banh bot loc" to "Bánh bột lọc",
    "Banh can" to "Bánh căn",
    "Banh canh" to "Bánh canh",
    "Banh chung" to "Bánh chưng",
    "Banh cuon" to "Bánh cuốn",
    "Banh duc" to "Bánh đúc",
    "Banh gio" to "Bánh giò",
    "Banh khot" to "Bánh khọt",
    "Banh mi" to "Bánh mì",
    "Banh pia" to "Bánh pía",
    "Banh tet" to "Bánh tét",
    "Banh trang nuong" to "Bánh tráng nướng",
    "Banh xeo" to "Bánh xèo",
    "Bun bo Hue" to "Bún bò Huế",
    "Bun dau mam tom" to "Bún đậu mắm tôm",
    "Bun mam" to "Bún mắm",
    "Bun rieu" to "Bún riêu",
    "Bun thit nuong" to "Bún thịt nướng",
    "Ca kho to" to "Cá kho tộ",
    "Canh chua" to "Canh chua",
    "Cao lau" to "Cao lầu",
    "Chao long" to "Cháo lòng",
    "Com tam" to "Cơm tấm",
    "Goi cuon" to "Gỏi cuốn",
    "Hu tieu" to "Hủ tiếu",
    "Mi quang" to "Mì Quảng",
    "Nem chua" to "Nem chua",
    "Pho" to "Phở",
    "Xoi xeo" to "Xôi xéo"
)

class FoodClassifierHelper(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()

    companion object {
        private const val MODEL_FILE = "food_recognition_model.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE = 224
        const val TOP_K = 5
    }

    init {
        loadModel()
        loadLabels()
    }

    private fun loadModel() {
        val afd = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(afd.fileDescriptor)
        val channel = inputStream.channel
        val model = channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        interpreter = Interpreter(model)
    }

    private fun loadLabels() {
        labels = context.assets.open(LABELS_FILE).bufferedReader().readLines()
            .filter { it.isNotBlank() }
    }

    fun classify(bitmap: Bitmap): List<ClassificationResult> {
        val interp = interpreter ?: return emptyList()
        val buffer = preprocessBitmap(bitmap)
        val output = Array(1) { FloatArray(labels.size) }
        interp.run(buffer, output)
        return output[0]
            .mapIndexed { i, score ->
                val rawLabel = labels.getOrElse(i) { "?" }
                ClassificationResult(labelToVietnamese[rawLabel] ?: rawLabel, score)
            }
            .sortedByDescending { it.confidence }
            .take(TOP_K)
    }

    private fun preprocessBitmap(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            // EfficientNetV2 expects raw [0, 255] float — rescaling is built into the model
            buffer.putFloat((pixel shr 16 and 0xFF).toFloat()) // R
            buffer.putFloat((pixel shr 8 and 0xFF).toFloat())  // G
            buffer.putFloat((pixel and 0xFF).toFloat())         // B
        }
        buffer.rewind()
        return buffer
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
