package com.vitalai.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

fun uriToFile(context: Context, uri: Uri, prefix: String = "upload_", suffix: String = ".jpg"): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)
        tempFile.outputStream().use { inputStream.copyTo(it) }
        inputStream.close()
        tempFile
    } catch (e: Exception) {
        Log.e("UriUtils", "Failed to convert Uri to File: ${e.localizedMessage}", e)
        null
    }
}
