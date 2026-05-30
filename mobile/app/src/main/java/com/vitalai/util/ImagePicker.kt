package com.vitalai.util

import android.content.Context
import android.net.Uri
import java.io.File

/** A local copy of a user-picked image, ready for multipart upload. */
data class PickedImage(
    val file: File,
    val mimeType: String
)

/**
 * Copies the content behind [uri] into a cache file so it can be uploaded as a
 * multipart body. Returns null if the image cannot be read. Shared by every
 * screen that uploads an image (avatar, custom food, …) so the logic lives in
 * one place.
 */
fun copyUriToCacheFile(context: Context, uri: Uri, prefix: String = "upload"): PickedImage? {
    return try {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> "jpg"
        }
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        PickedImage(file, mimeType)
    } catch (_: Exception) {
        null
    }
}
