package com.audio.mp3cutter.ringtone.maker.utils

import java.util.Locale

object FileUtils {
    fun formatDuration(durationMs: Long): String {
        val minutes = (durationMs / 1000) / 60
        val seconds = (durationMs / 1000) % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    // Format with decimal precision (0.1 seconds) for editing accuracy
    fun formatDurationWithDecimal(durationMs: Long): String {
        val totalSeconds = durationMs / 1000.0
        val minutes = (totalSeconds / 60).toInt()
        val seconds = totalSeconds % 60
        val secondsInt = seconds.toInt()
        val secondsDecimal = ((seconds - secondsInt) * 10).toInt()
        return String.format(
                Locale.getDefault(),
                "%02d:%02d.%01d",
                minutes,
                secondsInt,
                secondsDecimal
        )
    }

    fun formatFileSize(sizeBytes: Long): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return String.format(Locale.getDefault(), "%.1f MB", mb)
    }

    fun getAudioFromUri(
            context: android.content.Context,
            uri: android.net.Uri
    ): com.audio.mp3cutter.ringtone.maker.data.model.AudioModel? {
        var audioModel: com.audio.mp3cutter.ringtone.maker.data.model.AudioModel? = null
        try {
            val projection =
                    arrayOf(
                            android.provider.OpenableColumns.DISPLAY_NAME,
                            android.provider.OpenableColumns.SIZE
                    )
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex =
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)

                    val name = if (nameIndex != -1) cursor.getString(nameIndex) else "Unknown"
                    val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L

                    // Copy to cache to get a valid file path and duration
                    val cachedFile = copyUriToCache(context, uri, name)
                    if (cachedFile != null) {
                        val duration = getDuration(cachedFile.absolutePath)
                        audioModel =
                                com.audio.mp3cutter.ringtone.maker.data.model.AudioModel(
                                        id = System.currentTimeMillis(), // Temp ID
                                        path = cachedFile.absolutePath,
                                        title = name,
                                        artist = "Imported",
                                        duration = duration,
                                        size = size,
                                        contentUri = uri.toString()
                                )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return audioModel
    }

    private fun copyUriToCache(
            context: android.content.Context,
            uri: android.net.Uri,
            fileName: String
    ): java.io.File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = java.io.File(context.cacheDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getDuration(path: String): Long {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(path)
            val time =
                    retriever.extractMetadata(
                            android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                    )
            retriever.release()
            time?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
