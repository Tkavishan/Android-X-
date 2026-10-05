package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SelectedFileInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val isVaultXFile: Boolean = name.endsWith(".vaultx", ignoreCase = true)
) {
    val formattedSize: String get() = FileUtils.formatFileSize(sizeBytes)
}

data class DownloadOutputTarget(
    val uri: Uri,
    val displayPath: String,
    val outputStream: OutputStream
)

object FileUtils {
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[digitGroups]}"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun queryFileInfo(context: Context, uri: Uri): SelectedFileInfo {
        var name = "selected_file"
        var size: Long = 0
        var mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex)
                }
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        // Fallback for file:// or unqueryable size
        if (size <= 0) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    size = afd.length
                }
            } catch (_: Exception) {}
        }

        return SelectedFileInfo(
            uri = uri,
            name = name,
            sizeBytes = size,
            mimeType = mime
        )
    }

    /**
     * Creates an output stream directly in the device's public Downloads directory.
     * Uses MediaStore.Downloads on Android 10+ (API 29+) with zero storage permissions required.
     */
    fun createDownloadTarget(
        context: Context,
        fileName: String,
        mimeType: String = "application/octet-stream"
    ): DownloadOutputTarget {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            var insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (insertUri == null) {
                // If collision or error, append timestamp to make unique
                val base = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
                val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""
                val uniqueName = "${base}_${System.currentTimeMillis()}$ext"
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueName)
                insertUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("Failed to create entry in Downloads")
            }

            val stream = resolver.openOutputStream(insertUri, "wt")
                ?: resolver.openOutputStream(insertUri)
                ?: throw IOException("Failed to open output stream in Downloads")

            return DownloadOutputTarget(
                uri = insertUri,
                displayPath = "Downloads/$fileName",
                outputStream = stream
            )
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            var targetFile = File(downloadsDir, fileName)
            if (targetFile.exists()) {
                val base = if (fileName.contains('.')) fileName.substringBeforeLast('.') else fileName
                val ext = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""
                targetFile = File(downloadsDir, "${base}_${System.currentTimeMillis()}$ext")
            }
            val stream = FileOutputStream(targetFile)
            val uri = Uri.fromFile(targetFile)
            return DownloadOutputTarget(
                uri = uri,
                displayPath = targetFile.absolutePath,
                outputStream = stream
            )
        }
    }

    /**
     * Marks the file in Downloads as completed (clearing IS_PENDING so other apps can access it immediately).
     * Must be called AFTER the output stream has been closed.
     */
    fun finalizeDownloadTarget(context: Context, uri: Uri): String {
        var finalPath = "Downloads"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (_: Exception) {}

            try {
                context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                        val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                        if (nameCol != -1 && !cursor.isNull(nameCol)) {
                            finalPath = "Downloads/${cursor.getString(nameCol)}"
                        }
                        if (dataCol != -1 && !cursor.isNull(dataCol)) {
                            val diskPath = cursor.getString(dataCol)
                            if (!diskPath.isNullOrBlank()) {
                                MediaScannerConnection.scanFile(context, arrayOf(diskPath), null, null)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        } else {
            try {
                val path = uri.path
                if (path != null) {
                    finalPath = path
                    MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                }
            } catch (_: Exception) {}
        }
        return finalPath
    }
}
