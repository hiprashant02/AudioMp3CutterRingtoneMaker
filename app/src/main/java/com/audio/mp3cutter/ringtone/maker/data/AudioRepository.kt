package com.audio.mp3cutter.ringtone.maker.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.audio.mp3cutter.ringtone.maker.data.model.AudioModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class AudioSortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("A to Z"),
    NAME_DESC("Z to A"),
    SIZE_DESC("Largest First"),
    DURATION_DESC("Longest First")
}

class AudioRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getAudioChunk(
        limit: Int, 
        offset: Int, 
        query: String? = null,
        sortOption: AudioSortOption = AudioSortOption.DATE_DESC
    ): List<AudioModel> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioModel>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA
        )

        // Base selection: Music files with any duration > 0
        var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"

        // Append search query if present
        var selectionArgs: Array<String>? = null
        if (!query.isNullOrBlank()) {
            selection += " AND (${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?)"
            val searchPattern = "%$query%"
            selectionArgs = arrayOf(searchPattern, searchPattern)
        }
        
        // Construct Order By clause based on Sort Option
        val orderBy = when (sortOption) {
            AudioSortOption.DATE_DESC -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            AudioSortOption.DATE_ASC -> "${MediaStore.Audio.Media.DATE_ADDED} ASC"
            AudioSortOption.NAME_ASC -> "${MediaStore.Audio.Media.TITLE} ASC"
            AudioSortOption.NAME_DESC -> "${MediaStore.Audio.Media.TITLE} DESC"
            AudioSortOption.SIZE_DESC -> "${MediaStore.Audio.Media.SIZE} DESC"
            AudioSortOption.DURATION_DESC -> "${MediaStore.Audio.Media.DURATION} DESC"
        }
        
        // Add Limit and Offset for pagination
        // Note: Sort order must be consistent for pagination to work correctly
        val sortOrder = "$orderBy LIMIT $limit OFFSET $offset"

        try {
            // On Android Q and above, we can use Bundle for LIMIT/OFFSET, but sticking to string manipulation 
            // for broader compatibility or simplifying the query string if the specific API level allows.
            // However, typical ContentResolver query doesn't standardly support LIMIT/OFFSET in the sortOrder string on all versions safely.
            // A common workaround for MediaStore is using the sortOrder string hack, which works on many devices.
            // Alternatively, for Android O+, we can use the Bundle query overload.
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val args = android.os.Bundle().apply {
                    putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
                    
                    // Parse Sort Columns for Bundle
                     val (col, dir) = when (sortOption) {
                        AudioSortOption.DATE_DESC -> MediaStore.Audio.Media.DATE_ADDED to android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                        AudioSortOption.DATE_ASC -> MediaStore.Audio.Media.DATE_ADDED to android.content.ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                        AudioSortOption.NAME_ASC -> MediaStore.Audio.Media.TITLE to android.content.ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                        AudioSortOption.NAME_DESC -> MediaStore.Audio.Media.TITLE to android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                        AudioSortOption.SIZE_DESC -> MediaStore.Audio.Media.SIZE to android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                        AudioSortOption.DURATION_DESC -> MediaStore.Audio.Media.DURATION to android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    }
                    
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(col))
                    putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, dir)
                    
                    putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                     if (selectionArgs != null) {
                        putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    }
                }
                
                context.contentResolver.query(collection, projection, args, null)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    // DATA column is deprecated in Q+, but usually still accessible for read if we have permission or for relative path
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn)
                        val artist = cursor.getString(artistColumn)
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val path = cursor.getString(dataColumn)

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        audioList.add(
                            AudioModel(
                                id = id,
                                title = title ?: "Unknown",
                                artist = if (artist == "<unknown>") "Unknown Artist" else artist ?: "Unknown Artist",
                                duration = duration,
                                size = size,
                                path = path,
                                contentUri = contentUri
                            )
                        )
                    }
                }
            } else {
                 // Fallback for older versions using sortOrder hack
                 context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs, // Pass args here
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn)
                        val artist = cursor.getString(artistColumn)
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val path = cursor.getString(dataColumn)

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        audioList.add(
                            AudioModel(
                                id = id,
                                title = title ?: "Unknown",
                                artist = if (artist == "<unknown>") "Unknown Artist" else artist ?: "Unknown Artist",
                                duration = duration,
                                size = size,
                                path = path,
                                contentUri = contentUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext audioList
    }

    /**
     * Gets recently exported/processed audio files from the app's output directory.
     * Searches for files that were created or modified by AudioStudio.
     * 
     * @param limit Maximum number of recent projects to return
     * @return List of recent AudioModel projects, sorted by date modified descending
     */
    suspend fun getRecentProjects(limit: Int = 10): List<AudioModel> = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioModel>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_MODIFIED
        )

        // Search for files in the Music folder that were likely created by this app
        // Look for files with "AudioStudio" in the path or typical export patterns
        val selection = """
            ${MediaStore.Audio.Media.DURATION} > 0 AND (
                ${MediaStore.Audio.Media.DATA} LIKE ? OR 
                ${MediaStore.Audio.Media.DATA} LIKE ? OR 
                ${MediaStore.Audio.Media.DATA} LIKE ? OR
                ${MediaStore.Audio.Media.DATA} LIKE ?
            )
        """.trimIndent()

        val selectionArgs = arrayOf(
            "%AudioStudio%",
            "%/Music/Audio%",
            "%_cut.%",
            "%_merged.%"
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val args = android.os.Bundle().apply {
                    putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, limit)
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SORT_COLUMNS, 
                        arrayOf(MediaStore.Audio.Media.DATE_MODIFIED))
                    putInt(android.content.ContentResolver.QUERY_ARG_SORT_DIRECTION, 
                        android.content.ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                    putString(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                    putStringArray(android.content.ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                }
                
                context.contentResolver.query(collection, projection, args, null)?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn)
                        val artist = cursor.getString(artistColumn)
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val path = cursor.getString(dataColumn)

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        audioList.add(
                            AudioModel(
                                id = id,
                                title = title ?: "Unknown",
                                artist = if (artist == "<unknown>") "AudioStudio Project" else artist ?: "AudioStudio Project",
                                duration = duration,
                                size = size,
                                path = path,
                                contentUri = contentUri
                            )
                        )
                    }
                }
            } else {
                val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC LIMIT $limit"
                
                context.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn)
                        val artist = cursor.getString(artistColumn)
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val path = cursor.getString(dataColumn)

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        ).toString()

                        audioList.add(
                            AudioModel(
                                id = id,
                                title = title ?: "Unknown",
                                artist = if (artist == "<unknown>") "AudioStudio Project" else artist ?: "AudioStudio Project",
                                duration = duration,
                                size = size,
                                path = path,
                                contentUri = contentUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext audioList
    }
}
